package service;

import committees.*;
import exceptions.*;
import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import model.*;

public final class PartySystem {

    private final CentralCommittee centralCommittee;
    private final Map<Division, DivisionalCommittee> divisionCommittees = new EnumMap<>(Division.class);
    private final Map<District, DistrictCommittee> districtCommittees = new EnumMap<>(District.class);
    private final List<Member> pendingApplications = new ArrayList<>();
    private final List<DonationRecord> donationHistory = new ArrayList<>();
    private final Member adminUser;

    private final Path dataDirectory;
    private final Path membersFile;
    private final Path pendingFile;
    private final Path donationsFile;
    private final Path donationHistoryFile;

    public PartySystem() {
        this(Path.of("data"));
    }

    public PartySystem(Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        membersFile = dataDirectory.resolve("data_members.csv");
        pendingFile = dataDirectory.resolve("data_pending.csv");
        donationsFile = dataDirectory.resolve("data_donations.txt");
        donationHistoryFile = dataDirectory.resolve("data_donations_history.csv");
        centralCommittee = new CentralCommittee("National Central Committee");
        buildCommitteeStructure();
        try {
            loadFromFiles();
        } catch (IOException e) {
            System.out.println("Failed to load saved data: " + e.getMessage());
        }

        adminUser = new Member("ADMIN", "System Administrator", "admin@party.org", "000", "admin123", "Administrator", 0, 0, false, true,
            new Address(District.Dhaka), Role.ADMIN, CommitteeLevel.CENTRAL);
        centralCommittee.addLeader(adminUser);
    }

    private void buildCommitteeStructure() {
        // Group districts by division
        Map<Division, List<District>> districtsByDivision = new EnumMap<>(Division.class);
        for (District district : District.values()) {
            Division division = district.getDivision();
            districtsByDivision.computeIfAbsent(division, k -> new ArrayList<>());
            districtsByDivision.get(division).add(district);
        }
        // Create committees
        for (Division division : districtsByDivision.keySet()) {
            DivisionalCommittee divCommittee = new DivisionalCommittee(division.name() + " Divisional Committee");
            divisionCommittees.put(division, divCommittee);
            centralCommittee.addDivisional(divCommittee);
            for (District district : districtsByDivision.get(division)) {
                DistrictCommittee distCommittee = new DistrictCommittee(district.name() + " District Committee");
                divCommittee.addDistrict(distCommittee);
                districtCommittees.put(district, distCommittee);
            }
        }
    }

    public CentralCommittee getCentralCommittee() {
        return centralCommittee;
    }
    public DivisionalCommittee getDivisionalCommittee(Division division) {
        return divisionCommittees.get(division);
    }
    public DistrictCommittee getDistrictCommittee(District district) {
        return districtCommittees.get(district);
    }

    public Member login(String email, String password) {
        if (email == null) return null;
        Member member = findByEmail(email);
        if (member != null && member.getPassword() != null && member.getPassword().equals(password)) {
            return member;
        }
        return null;
    }

    public boolean isAdmin(Member member) {
        return member != null && member.getRole() == Role.ADMIN;
    }

    public Member applyForMembership(Member newMember) throws DuplicateMemberException {
        if (newMember == null || newMember.getEmail() == null || newMember.getNationalId() == null) {
            return null;
        }
        if (findById(newMember.getNationalId()) != null) {
            throw new DuplicateMemberException("Duplicate National ID");
        }
        if (findByEmail(newMember.getEmail()) != null) {
            throw new DuplicateMemberException("Duplicate Email");
        }
        pendingApplications.add(newMember);
        return newMember;
    }

    public List<Member> getAllPendingApplications() {
        return new ArrayList<>(pendingApplications);
    }

    public boolean approveApplication(String nationalId) {
        Iterator<Member> it = pendingApplications.iterator();
        while (it.hasNext()) {
            Member member = it.next();
            if (member.getNationalId().equals(nationalId)) {
                member.setApproved(true);
                it.remove();
                District d = member.getAddress().getDistrict();
                DistrictCommittee dc = getDistrictCommittee(d);
                if (dc != null) {
                    dc.addMember(member);
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public boolean rejectApplication(String nationalId) {
        Iterator<Member> it = pendingApplications.iterator();
        while (it.hasNext()) {
            Member member = it.next();
            if (member.getNationalId().equals(nationalId)) {
                it.remove();
                return true;
            }
        }
        return false;
    }

    public boolean terminateMembershipID(String nationalId) {
        Member member = findById(nationalId);
        if (member == null || member.getRole() == Role.ADMIN) return false;
        removeFromAllPlacements(member);
        pendingApplications.remove(member);
        member.setApproved(false);
        return true;
    }

    public boolean terminateMembershipEmail(String email) {
        Member member = findByEmail(email);
        if (member == null || member.getRole() == Role.ADMIN) return false;
        return terminateMembershipID(member.getNationalId());
    }

    public boolean promoteToLeader(String nationalId, CommitteeLevel level, Role newRole, Division division, District district) {
        if (level == null || newRole == null || newRole == Role.MEMBER || newRole == Role.ADMIN) {
            return false;
        }
        Member member = findById(nationalId);
        if (member == null || !member.isApproved()) return false;
        if (level == CommitteeLevel.DIVISIONAL
                && division != null
                && member.getAddress().getDivision() != division) {
            return false;
        }
        if (district != null && member.getAddress().getDistrict() != district) {
            member.setAddress(new Address(district));
        }
        removeFromAllPlacements(member);
        member.setCommitteeLevel(level);
        member.setRole(newRole);
        switch (level) {
            case CENTRAL:
                centralCommittee.addLeader(member);
                break;
            case DIVISIONAL:
                if (division == null) division = member.getAddress().getDivision();
                DivisionalCommittee dc = getDivisionalCommittee(division);
                if (dc == null) return false;
                dc.addLeader(member);
                break;
            case DISTRICT:
                if (district == null) district = member.getAddress().getDistrict();
                DistrictCommittee dcomm = getDistrictCommittee(district);
                if (dcomm == null) return false;
                dcomm.addLeader(member);
                break;
            default:
                break;
        }
        return true;
    }

    public boolean demoteLeader(String email) {
        Member member = findByEmail(email);
        if (member == null || member.getRole() == Role.ADMIN || member.getRole() == Role.MEMBER) {
            return false;
        }
        removeFromAllPlacements(member);
        member.setRole(Role.MEMBER);
        member.setCommitteeLevel(CommitteeLevel.DISTRICT);
        getDistrictCommittee(member.getAddress().getDistrict()).addMember(member);
        return true;
    }

    public boolean declareElection(Committee committee, Member currentUser) {
        if (committee instanceof CentralCommittee) {
            return centralCommittee.getElection().declareElection(currentUser);
        } else if (committee instanceof DivisionalCommittee) {
            DivisionalCommittee divCommittee = (DivisionalCommittee) committee;
            return divCommittee.getElection().declareElection(currentUser);
        } else if (committee instanceof DistrictCommittee) {
            DistrictCommittee distCommittee = (DistrictCommittee) committee;
            return distCommittee.getElection().declareElection(currentUser);
        }
        return false;
    }

    public boolean closeElection(Committee committee, Member currentUser) {
        List<Member> formerLeaders;
        Election election;
        if (committee instanceof CentralCommittee) {
            formerLeaders = new ArrayList<>(centralCommittee.getLeaders());
            election = centralCommittee.getElection();
        } else if (committee instanceof DivisionalCommittee) {
            DivisionalCommittee divCommittee = (DivisionalCommittee) committee;
            formerLeaders = new ArrayList<>(divCommittee.getLeaders());
            election = divCommittee.getElection();
        } else if (committee instanceof DistrictCommittee) {
            DistrictCommittee distCommittee = (DistrictCommittee) committee;
            formerLeaders = new ArrayList<>(distCommittee.getLeaders());
            election = distCommittee.getElection();
        } else {
            return false;
        }

        if (!election.closeElection(currentUser)) {
            return false;
        }
        List<Member> winners = election.getWinners();
        for (Member former : formerLeaders) {
            if (former.getRole() == Role.ADMIN) continue;
            removeFromAllPlacements(former);
            if (!winners.contains(former)) {
                former.setRole(Role.MEMBER);
                former.setCommitteeLevel(CommitteeLevel.DISTRICT);
                getDistrictCommittee(former.getAddress().getDistrict()).addMember(former);
            }
        }
        for (Member winner : winners) {
            removeFromAllPlacements(winner);
            addLeaderToCommittee(committee, winner);
        }
        return true;
    }

    public boolean vote(Committee committee, Role role, Member candidate, Member voter) {
        if (committee instanceof CentralCommittee) {
            return ((CentralCommittee) committee).getElection().vote(voter, role, candidate);
        } else if (committee instanceof DivisionalCommittee) {
            return ((DivisionalCommittee) committee).getElection().vote(voter, role, candidate);
        } else if (committee instanceof DistrictCommittee) {
            return ((DistrictCommittee) committee).getElection().vote(voter, role, candidate);
        }
        return false;
    }

    public boolean applyForLeadership(Member member, Role desiredRole, CommitteeLevel level) {
        if (desiredRole == Role.MEMBER || !member.isApproved()) return false;
        if (level == null) return false;
        switch (level) {
            case CENTRAL:
                return getCentralCommittee().getElection().registerCandidate(desiredRole, member);
            case DIVISIONAL: {
                Division division = member.getAddress().getDivision();
                DivisionalCommittee dc = getDivisionalCommittee(division);
                if (dc == null) return false;
                return dc.getElection().registerCandidate(desiredRole, member);
            }
            case DISTRICT: {
                District district = member.getAddress().getDistrict();
                DistrictCommittee dct = getDistrictCommittee(district);
                if (dct == null) return false;
                return dct.getElection().registerCandidate(desiredRole, member);
            }
            default:
                return false;
        }
    }

    public Member findByEmail(String email) {
        if (email == null) return null;
        String emailLower = email.trim().toLowerCase(Locale.ROOT);
        for (Member m : pendingApplications) {
            if (m.getEmail() != null
                    && m.getEmail().trim().toLowerCase(Locale.ROOT).equals(emailLower)) {
                return m;
            }
        }
        // Central leaders
        for (Member m : centralCommittee.getLeaders()) {
            if (m.getEmail() != null
                    && m.getEmail().trim().toLowerCase(Locale.ROOT).equals(emailLower)) {
                return m;
            }
        }
        // Divisional leaders
        for (DivisionalCommittee div : divisionCommittees.values()) {
            for (Member m : div.getLeaders()) {
                if (m.getEmail() != null
                        && m.getEmail().trim().toLowerCase(Locale.ROOT).equals(emailLower)) {
                    return m;
                }
            }
        }
        // District leaders and members
        for (DistrictCommittee dist : districtCommittees.values()) {
            for (Member m : dist.getLeaders()) {
                if (m.getEmail() != null
                        && m.getEmail().trim().toLowerCase(Locale.ROOT).equals(emailLower)) {
                    return m;
                }
            }
            for (Member m : dist.getMembers()) {
                if (m.getEmail() != null
                        && m.getEmail().trim().toLowerCase(Locale.ROOT).equals(emailLower)) {
                    return m;
                }
            }
        }
        return null;
    }

    public Member findById(String nationalId) {
        if (nationalId == null) return null;
        nationalId = nationalId.trim();
        for (Member m : pendingApplications) {
            if (nationalId.equals(m.getNationalId())) return m;
        }
        // Central leaders
        for (Member m : centralCommittee.getLeaders()) {
            if (nationalId.equals(m.getNationalId())) return m;
        }
        // Divisional leaders
        for (DivisionalCommittee div : divisionCommittees.values()) {
            for (Member m : div.getLeaders()) {
                if (nationalId.equals(m.getNationalId())) return m;
            }
        }
        // District leaders and members
        for (DistrictCommittee dist : districtCommittees.values()) {
            for (Member m : dist.getLeaders()) {
                if (nationalId.equals(m.getNationalId())) return m;
            }
            for (Member m : dist.getMembers()) {
                if (nationalId.equals(m.getNationalId())) return m;
            }
        }
        return null;
    }

    public double getDonations(){
        return centralCommittee.getTotalDonations();
    }

    public DonationRecord recordDonation(Member member, double amount)
            throws InvalidDonationException {
        centralCommittee.addDonation(amount);
        if (member != null) {
            member.setDonation(member.getDonation() + amount);
            member.setHasDonated(true);
        }
        DonationRecord record = new DonationRecord(
                Instant.now(),
                member == null ? "Anonymous" : member.getName(),
                amount);
        donationHistory.add(0, record);
        return record;
    }

    public List<DonationRecord> getDonationHistory() {
        return new ArrayList<>(donationHistory);
    }

    public List<Member> getAllApprovedMembers() {
        LinkedHashMap<String, Member> uniqueMembers = new LinkedHashMap<>();
        collectApprovedMembers(centralCommittee.getLeaders(), uniqueMembers);
        for (DivisionalCommittee committee : divisionCommittees.values()) {
            collectApprovedMembers(committee.getLeaders(), uniqueMembers);
        }
        for (DistrictCommittee committee : districtCommittees.values()) {
            collectApprovedMembers(committee.getLeaders(), uniqueMembers);
            collectApprovedMembers(committee.getMembers(), uniqueMembers);
        }
        List<Member> members = new ArrayList<>(uniqueMembers.values());
        members.sort(Comparator.comparing(Member::getName, String.CASE_INSENSITIVE_ORDER));
        return members;
    }

    public List<Member> getAllLeaders() {
        List<Member> leaders = new ArrayList<>();
        for (Member member : getAllApprovedMembers()) {
            if (member.getRole() != Role.MEMBER) {
                leaders.add(member);
            }
        }
        return leaders;
    }

    public SystemStats getStats() {
        return new SystemStats(
                getAllApprovedMembers().size(),
                pendingApplications.size(),
                getAllLeaders().size(),
                countActiveElections(),
                centralCommittee.getTotalDonations());
    }

    public void saveToFiles() {
        try {
            File membersOutputFile = membersFile.toFile();
            File pendingOutputFile = pendingFile.toFile();
            File donationsOutputFile = donationsFile.toFile();
            File parent = membersOutputFile.getParentFile();
            if (parent != null) {
                parent.mkdirs();
            }

            List<Member> approvedMembers = new ArrayList<>();
            // Central leaders
            for (Member m : centralCommittee.getLeaders()) {
                if (m.getRole() != Role.ADMIN && m.isApproved()) approvedMembers.add(m);
            }
            // Divisional leaders
            for (DivisionalCommittee div : divisionCommittees.values()) {
                for (Member m : div.getLeaders()) {
                    if (m.getRole() != Role.ADMIN && m.isApproved()) approvedMembers.add(m);
                }
            }
            // District leaders and members
            for (DistrictCommittee dist : districtCommittees.values()) {
                for (Member m : dist.getLeaders()) {
                    if (m.getRole() != Role.ADMIN && m.isApproved()) approvedMembers.add(m);
                }
                for (Member m : dist.getMembers()) {
                    if (m.getRole() != Role.ADMIN && m.isApproved() && m.getRole()==Role.MEMBER) approvedMembers.add(m);
                }
            }
            approvedMembers.sort(null);
            try (PrintWriter out = new PrintWriter(membersOutputFile)) {
                // Format: nid,name,email,phone,password,profession,yearlyIncome,donation,hasDonated,isApproved,division,district,role,committeeLevel
                for (Member member : approvedMembers) {
                    out.print(member.getNationalId()); out.print(",");
                    out.print(member.getName()); out.print(",");
                    out.print(member.getEmail()); out.print(",");
                    out.print(member.getPhone()); out.print(",");
                    out.print(member.getPassword()); out.print(",");
                    out.print(member.getProfession()); out.print(",");
                    out.print(member.getYearlyIncome()); out.print(",");
                    out.print(member.getDonation()); out.print(",");
                    out.print(member.hasDonated()); out.print(",");
                    out.print(member.isApproved()); out.print(",");
                    out.print(member.getAddress().getDivision().name()); out.print(",");
                    out.print(member.getAddress().getDistrict().name()); out.print(",");
                    out.print(member.getRole().name()); out.print(",");
                    out.print(member.getCommitteeLevel().name()); out.println();
                }
            }

            // Sort pending applications by name
            List<Member> pendingSorted = new ArrayList<>(pendingApplications);
            pendingSorted.sort(null);
            try (PrintWriter out = new PrintWriter(pendingOutputFile)) {
                // Format similar to members file (division,district)
                for (Member member : pendingSorted) {
                    out.print(member.getNationalId()); out.print(",");
                    out.print(member.getName()); out.print(",");
                    out.print(member.getEmail()); out.print(",");
                    out.print(member.getPhone()); out.print(",");
                    out.print(member.getPassword()); out.print(",");
                    out.print(member.getProfession()); out.print(",");
                    out.print(member.getYearlyIncome()); out.print(",");
                    out.print(member.getDonation()); out.print(",");
                    out.print(member.hasDonated()); out.print(",");
                    out.print(member.isApproved()); out.print(",");
                    out.print(member.getAddress().getDivision().name()); out.print(",");
                    out.print(member.getAddress().getDistrict().name()); out.print(",");
                    out.print(member.getRole().name()); out.print(",");
                    out.print(member.getCommitteeLevel().name()); out.println();
                }
            }
            try (PrintWriter out = new PrintWriter(donationsOutputFile)) {
                out.print(centralCommittee.getTotalDonations());
            }
            try (PrintWriter out = new PrintWriter(donationHistoryFile.toFile())) {
                List<DonationRecord> chronologicalHistory = new ArrayList<>(donationHistory);
                Collections.reverse(chronologicalHistory);
                for (DonationRecord record : chronologicalHistory) {
                    out.print(record.getTimestamp());
                    out.print(",");
                    out.print(sanitizeCsvValue(record.getDonor()));
                    out.print(",");
                    out.println(record.getAmount());
                }
            }
        } catch (IOException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    public final void loadFromFiles() throws IOException {
        centralCommittee.getLeaders().clear();
        for (DivisionalCommittee div : divisionCommittees.values()) {
            div.getLeaders().clear();
        }
        for (DistrictCommittee dist : districtCommittees.values()) {
            dist.getLeaders().clear();
            dist.getMembers().clear();
        }
        pendingApplications.clear();

        donationHistory.clear();

        File membersInputFile = membersFile.toFile();
        if (membersInputFile.exists()) {
            try (Scanner scanner = new Scanner(membersInputFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] p = line.split(",", -1);
                    if (p.length < 14) {
                        continue;
                    }
                    try {
                        String nid = p[0];
                        String name = p[1];
                        String email = p[2];
                        String phone = p[3];
                        String password = p[4];
                        String profession = p[5];
                        double yearlyIncome = Double.parseDouble(p[6]);
                        double donation = Double.parseDouble(p[7]);
                        boolean hasDonated = Boolean.parseBoolean(p[8]);
                        boolean approved = Boolean.parseBoolean(p[9]);
                        Division division = Division.valueOf(p[10]);
                        District district = District.valueOf(p[11]);
                        Role role = Role.valueOf(p[12]);
                        CommitteeLevel cl = CommitteeLevel.valueOf(p[13]);
                        Member member = new Member(nid, name, email, phone, password, profession, yearlyIncome, donation, hasDonated, approved, new Address(district), role, cl);
                        if (approved) {
                            switch (cl) {
                                case CENTRAL:
                                    centralCommittee.addLeader(member);
                                    break;
                                case DIVISIONAL:
                                    getDivisionalCommittee(division).addLeader(member);
                                    break;
                                case DISTRICT:
                                    if (role == Role.MEMBER) {
                                        getDistrictCommittee(district).addMember(member);
                                    } else {
                                        getDistrictCommittee(district).addLeader(member);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    } catch (Exception e) {
                        System.out.println("Error loading a member line: " + e.getMessage());
                    }
                }
            }
        }

        File pendingInputFile = pendingFile.toFile();
        if (pendingInputFile.exists()) {
            try (Scanner scanner = new Scanner(pendingInputFile)) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    if (line.isBlank()) {
                        continue;
                    }
                    String[] p = line.split(",", -1);
                    if (p.length < 14) {
                        continue;
                    }
                    try {
                        String nid = p[0];
                        String name = p[1];
                        String email = p[2];
                        String phone = p[3];
                        String password = p[4];
                        String profession = p[5];
                        double yearlyIncome = Double.parseDouble(p[6]);
                        double donation = Double.parseDouble(p[7]);
                        boolean hasDonated = Boolean.parseBoolean(p[8]);
                        boolean approved = Boolean.parseBoolean(p[9]);
                        Division division = Division.valueOf(p[10]);
                        District district = District.valueOf(p[11]);
                        Role role = Role.valueOf(p[12]);
                        CommitteeLevel cl = CommitteeLevel.valueOf(p[13]);
                        Member member = new Member(nid, name, email, phone, password, profession, yearlyIncome, donation, hasDonated, approved, new Address(district), role, cl);
                        if (!approved) {
                            pendingApplications.add(member);
                        }
                    } catch (Exception e) {
                        System.out.println("Error loading a pending line: " + e.getMessage());
                    }
                }
            }
        }

        File donationsInputFile = donationsFile.toFile();
        if (donationsInputFile.exists()) {
            try (Scanner scanner = new Scanner(donationsInputFile)) {
                if (scanner.hasNextDouble()) {
                    double donations = scanner.nextDouble();
                    centralCommittee.setTotalDonations(donations);
                }
            } catch (Exception e) {
                System.out.println("Donations file load failed: " + e.getMessage());
            }
        }

        File historyInputFile = donationHistoryFile.toFile();
        if (historyInputFile.exists()) {
            try (Scanner scanner = new Scanner(historyInputFile)) {
                while (scanner.hasNextLine()) {
                    String[] values = scanner.nextLine().split(",", 3);
                    if (values.length != 3) continue;
                    DonationRecord record = new DonationRecord(
                            Instant.parse(values[0]),
                            values[1],
                            Double.parseDouble(values[2]));
                    donationHistory.add(0, record);
                }
            } catch (Exception e) {
                System.out.println("Donation history load failed: " + e.getMessage());
            }
        }
    }

    private void removeFromAllPlacements(Member member) {
        centralCommittee.getLeaders().remove(member);
        for (DivisionalCommittee committee : divisionCommittees.values()) {
            committee.getLeaders().remove(member);
        }
        for (DistrictCommittee committee : districtCommittees.values()) {
            committee.getLeaders().remove(member);
            committee.getMembers().remove(member);
        }
    }

    private void addLeaderToCommittee(Committee committee, Member member) {
        if (committee instanceof CentralCommittee) {
            ((CentralCommittee) committee).addLeader(member);
        } else if (committee instanceof DivisionalCommittee) {
            ((DivisionalCommittee) committee).addLeader(member);
        } else if (committee instanceof DistrictCommittee) {
            ((DistrictCommittee) committee).addLeader(member);
        }
    }

    private void collectApprovedMembers(
            List<Member> source,
            Map<String, Member> destination) {
        for (Member member : source) {
            if (member.isApproved() && member.getRole() != Role.ADMIN) {
                destination.putIfAbsent(member.getNationalId(), member);
            }
        }
    }

    private int countActiveElections() {
        int activeElections = centralCommittee.getElection().isDeclared() ? 1 : 0;
        for (DivisionalCommittee committee : divisionCommittees.values()) {
            if (committee.getElection().isDeclared()) activeElections++;
        }
        for (DistrictCommittee committee : districtCommittees.values()) {
            if (committee.getElection().isDeclared()) activeElections++;
        }
        return activeElections;
    }

    private String sanitizeCsvValue(String value) {
        return value == null ? "" : value.replace(",", " ");
    }
}
