package committees;

import java.util.ArrayList;
import java.util.List;
import model.CommitteeLevel;
import model.Member;
import model.Role;
import service.Election;

public final class DivisionalCommittee extends Committee implements CommitteeOperations {
    private final List<DistrictCommittee> district;
    private final List<Member> divisionalCommitteeLeader;
    private final CommitteeLevel level = CommitteeLevel.DIVISIONAL;
    private Election election;

    public DivisionalCommittee(String name) {
        super(name);
        this.district = new ArrayList<>();
        this.divisionalCommitteeLeader = new ArrayList<>();
        this.election = new Election(this);
    }

    public CommitteeLevel getCommitteeLevel(){
        return level;
    }

    public void addDistrict(DistrictCommittee d) {
        if (d != null) district.add(d);
    }

    public List<DistrictCommittee> getDistricts() {
        return district;
    }

    @Override
    public void addLeader(Member m) {
        if (m != null && !divisionalCommitteeLeader.contains(m)) {
            divisionalCommitteeLeader.add(m);
        }
    }

    public void addLeader(Member m, Role role, CommitteeLevel level){
        if (m == null) return;
        m.setRole(role);
        m.setCommitteeLevel(level);
        addLeader(m);
    }

    @Override
    public boolean removeLeader(Member m) {
        return divisionalCommitteeLeader.remove(m);
    }

    @Override
    public List<Member> getLeaders() {
        return divisionalCommitteeLeader;
    }

    @Override
    public void assignRole(Member m, Role role) {
        if (m != null && divisionalCommitteeLeader.contains(m)) {
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
        divisionalCommitteeLeader.sort(null);
        System.out.println("Divisional Committee: " + getCommitteeName());
        if (divisionalCommitteeLeader.isEmpty()) {
            System.out.println("No leaders in this divisional committee.");
        } else {
            for (Member member : divisionalCommitteeLeader) {
                System.out.println(member.ProfessionalInfo());
            }
        }
    }
}

