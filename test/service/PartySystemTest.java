package service;

import committees.CentralCommittee;
import exceptions.DuplicateMemberException;
import java.nio.file.Files;
import java.nio.file.Path;
import model.Address;
import model.CommitteeLevel;
import model.District;
import model.Division;
import model.Member;
import model.Role;

public final class PartySystemTest {
    private int assertions;

    public static void main(String[] args) throws Exception {
        new PartySystemTest().run();
    }

    private void run() throws Exception {
        Path dataDirectory = Files.createTempDirectory("ppm-test-");
        PartySystem system = new PartySystem(dataDirectory);
        Member member = createMember("NID-100", "member@example.com");

        check(system.getStats().getApprovedMembers() == 0, "new system starts without members");
        check(system.applyForMembership(member) == member, "application is accepted");
        check(system.getStats().getPendingApplications() == 1, "pending count is updated");
        check(system.login(member.getEmail(), "secret") == member, "pending account can be identified");
        check(!member.isApproved(), "pending account remains unapproved");

        expectDuplicate(() -> system.applyForMembership(
                createMember("NID-100", "different@example.com")));
        expectDuplicate(() -> system.applyForMembership(
                createMember("NID-200", "MEMBER@example.com")));

        check(system.approveApplication(member.getNationalId()), "application can be approved");
        check(member.isApproved(), "approved flag is updated");
        check(system.getAllApprovedMembers().size() == 1, "directory contains approved member");

        system.recordDonation(member, 1250);
        check(system.getDonations() == 1250, "donation total is updated");
        check(member.getDonation() == 1250, "member donation total is updated");
        check(system.getDonationHistory().size() == 1, "donation history is updated");

        CentralCommittee centralCommittee = system.getCentralCommittee();
        Member admin = system.login("admin@party.org", "admin123");
        check(system.declareElection(centralCommittee, admin), "admin can declare central election");
        check(system.getStats().getActiveElections() == 1, "active election is counted");
        check(system.applyForLeadership(member, Role.PRESIDENT, CommitteeLevel.CENTRAL),
                "approved member can register as a candidate");
        check(centralCommittee.getElection().vote(admin, Role.PRESIDENT, member),
                "a valid vote is recorded");
        check(system.closeElection(centralCommittee, admin), "election can be closed");
        check(member.getRole() == Role.PRESIDENT, "winner receives elected role");
        check(centralCommittee.getLeaders().contains(admin), "administrator remains in committee");
        check(centralCommittee.getLeaders().contains(member), "winner joins committee");

        check(system.demoteLeader(member.getEmail()), "leader can be demoted");
        check(member.getRole() == Role.MEMBER, "demoted leader becomes a member");
        check(system.getDistrictCommittee(District.Dhaka).getMembers().contains(member),
                "demoted leader returns to district membership");
        check(system.promoteToLeader(
                        member.getNationalId(),
                        CommitteeLevel.DISTRICT,
                        Role.PRESIDENT,
                        Division.Dhaka,
                        District.Dhaka),
                "member can be assigned as a district president");
        check(system.declareElection(system.getDistrictCommittee(District.Dhaka), member),
                "district president can declare a district election");
        check(system.closeElection(system.getDistrictCommittee(District.Dhaka), member),
                "district president can close a district election");
        check(member.getRole() == Role.PRESIDENT,
                "leadership is retained when an election has no candidates");

        system.saveToFiles();
        PartySystem reloaded = new PartySystem(dataDirectory);
        check(reloaded.findByEmail(member.getEmail()) != null, "member persists to disk");
        check(reloaded.getDonations() == 1250, "donation total persists to disk");
        check(reloaded.getDonationHistory().size() == 1, "donation history persists to disk");
        check(reloaded.terminateMembershipEmail(member.getEmail()), "member can be terminated");
        check(reloaded.findByEmail(member.getEmail()) == null, "terminated member is removed");

        System.out.println("PartySystemTest passed: " + assertions + " assertions");
    }

    private Member createMember(String nationalId, String email) {
        return new Member(
                nationalId,
                "Test Member",
                email,
                "01700000000",
                "secret",
                "Student",
                100000,
                false,
                false,
                new Address(District.Dhaka),
                Role.MEMBER,
                CommitteeLevel.DISTRICT);
    }

    private void expectDuplicate(CheckedAction action) throws Exception {
        try {
            action.run();
            throw new AssertionError("Expected DuplicateMemberException");
        } catch (DuplicateMemberException expected) {
            assertions++;
        }
    }

    private void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        assertions++;
    }

    @FunctionalInterface
    private interface CheckedAction {
        void run() throws Exception;
    }
}
