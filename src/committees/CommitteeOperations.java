package committees;

import java.util.List;
import model.Member;
import model.Role;

public interface CommitteeOperations {
    void addLeader(Member m);
    boolean removeLeader(Member m);
    List<Member> getLeaders();
    void displayInfo();
    void assignRole(Member m, Role role);
}
