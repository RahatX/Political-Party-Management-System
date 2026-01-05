package committees;

import exceptions.InvalidDonationException;
import java.util.*;
import model.CommitteeLevel;
import model.Member;
import model.Role;
import service.Election;

public class CentralCommittee extends Committee implements CommitteeOperations {

    private final List<DivisionalCommittee> divisionals;
    private final List<Member> centralCommitteeLeader;
    private final CommitteeLevel level = CommitteeLevel.CENTRAL;
    private Election election;
    private double donations;

    public CentralCommittee(String name) {
        super(name);
        this.divisionals = new ArrayList<>();
        this.centralCommitteeLeader = new ArrayList<>();
        this.election = new Election(this);
        this.donations = 0;
    }

    public void addDivisional(DivisionalCommittee d) {
        if (d != null) divisionals.add(d);
    }

    public List<DivisionalCommittee> getDivisionals() {
        return divisionals;
    }

    public CommitteeLevel getCommitteeLevel(){
        return level;
    }

    @Override
    public void addLeader(Member m) {
        if (m != null) centralCommitteeLeader.add(m);
    }

    public void addLeader(Member m, Role role, CommitteeLevel level){
        if (m == null) return;
        m.setRole(role);
        m.setCommitteeLevel(level);
        centralCommitteeLeader.add(m);
    }

    @Override
    public boolean removeLeader(Member m) {
        return centralCommitteeLeader.remove(m);
    }

    @Override
    public List<Member> getLeaders() {
        return centralCommitteeLeader;
    }

    @Override
    public void assignRole(Member m, Role role) {
        if (m != null && centralCommitteeLeader.contains(m)) {
            m.setRole(role);
        }
    }

    @Override
    public Election getElection() {
        return election;
    }

    public void startNewElection() {
        this.election = new Election(this);
    }

    public void addDonation(double amt) throws InvalidDonationException {
        if (amt <= 0) throw new InvalidDonationException("Donation amount must be positive");
        donations += amt;
    }

    public double getTotalDonations() {
        return donations;
    }

    public void setTotalDonations(double donations){
        this.donations = donations;
    }

    @Override
    public void displayInfo() {
        centralCommitteeLeader.sort(null);
        System.out.println("Central Committee: " + getCommitteeName());
        System.out.println("Total Donations: " + donations);
        if (centralCommitteeLeader.isEmpty()) {
            System.out.println("No leaders in the central committee.");
        } else {
            for (Member member : centralCommitteeLeader) {
                System.out.println(member.ProfessionalInfo());
            }
        }
    }
}
