package committees;

import java.util.ArrayList;
import java.util.List;
import model.CommitteeLevel;
import model.Member;
import model.Role;
import service.Election;

public class DistrictCommittee extends Committee implements CommitteeOperations {
    private final List<Member> districtCommitteeLeader;
    private final List<Member> member;
    private final CommitteeLevel level = CommitteeLevel.DISTRICT;
    private Election election;

    public DistrictCommittee(String name) {
        super(name);
        this.member = new ArrayList<>();
        this.districtCommitteeLeader = new ArrayList<>();
        this.election = new Election(this);
    }

    public CommitteeLevel getCommitteeLevel(){
        return level;
    }

    public void addMember(Member m) {
        if (m != null) member.add(m);
    }

    public void addMember(Member m, Role role, CommitteeLevel level){
        if (m == null) return;
        m.setRole(role);
        m.setCommitteeLevel(level);
        member.add(m);
    }

    public boolean removeMember(Member m) {
        return member.remove(m);
    }

    public List<Member> getMembers() {
        return member;
    }

    @Override
    public void addLeader(Member m) {
        if (m != null) districtCommitteeLeader.add(m);
    }

    public void addLeader(Member m, Role role, CommitteeLevel level){
        if (m == null) return;
        m.setRole(role);
        m.setCommitteeLevel(level);
        districtCommitteeLeader.add(m);
    }

    @Override
    public boolean removeLeader(Member m) {
        return districtCommitteeLeader.remove(m);
    }

    @Override
    public List<Member> getLeaders() {
        return districtCommitteeLeader;
    }

    @Override
    public void assignRole(Member m, Role role) {
        if (m != null && districtCommitteeLeader.contains(m)) {
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

    @Override
    public void displayInfo() {
        districtCommitteeLeader.sort(null);
        member.sort(null);
        System.out.println("District Committee: " + getCommitteeName());
        if (districtCommitteeLeader.isEmpty()) {
            System.out.println("No leaders in this district committee.");
        } else {
            for (Member m : districtCommitteeLeader) {
                System.out.println(m.ProfessionalInfo());
            }
        }
        System.out.println(); // spacer
        System.out.println("Members of District: " + getCommitteeName());
        if (member.isEmpty()) {
            System.out.println("No members in this district.");
        } else {
            for (Member m : member) {
                if (m != null && m.isApproved()) {
                    System.out.println(m.ProfessionalInfo());
                }
            }
        }
    }
}
