package service;

import committees.CentralCommittee;
import committees.Committee;
import committees.DistrictCommittee;
import committees.DivisionalCommittee;
import java.util.*;
import model.CommitteeLevel;
import model.Member;
import model.Role;

public class Election {
    private final Committee committee;
    private final Map<Role, List<Member>> candidates;
    private final List<Member> winners;
    private final Map<Role, Map<Member, Integer>> votes;
    private final Map<Member, Set<Role>> voterRoleVotes;
    private boolean declared;

    public Election(Committee committee) {
        this.committee = committee;
        this.candidates = new EnumMap<>(Role.class);
        this.winners = new ArrayList<>();
        this.votes = new EnumMap<>(Role.class);
        this.voterRoleVotes = new HashMap<>();
        this.declared = false;
    }

    /**
     * Register a candidate for a role.
     * Registration is allowed ONLY when an election is declared (open).
     */
    public boolean registerCandidate(Role role, Member candidate) {
        if (!declared) {
            return false; //election must be declared to register candidates
        }
        if (role == null || candidate == null)
        	{
        	return false;
        	}
        if (role == Role.MEMBER || role == Role.ADMIN) {
            return false;
        }
        candidates.computeIfAbsent(role, k -> new ArrayList<>());
        List<Member> list = candidates.get(role);
        if (list.contains(candidate)) {
            return false;
        }
        
        //ideally ensure candidate is approved and belongs to this committee; minimally check approved
        
        if (!candidate.isApproved()) return false;
        list.add(candidate);
        return true;
    }

    public boolean declareElection(Member currentUser) {
        CommitteeLevel level = null;
        if (committee instanceof CentralCommittee) {
            level = CommitteeLevel.CENTRAL;
        } else if (committee instanceof DivisionalCommittee) {
            level = CommitteeLevel.DIVISIONAL;
        } else if (committee instanceof DistrictCommittee) {
            level = CommitteeLevel.DISTRICT;
        }
        if (currentUser == null) return false;
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PRESIDENT) {
            return false;
        }
        
        //permission checks based on committee level
        
        if (level == CommitteeLevel.CENTRAL && currentUser.getCommitteeLevel() != CommitteeLevel.CENTRAL) {
            return false;
        }
        if (level == CommitteeLevel.DIVISIONAL && currentUser.getCommitteeLevel() != CommitteeLevel.CENTRAL) {
            return false;
        }
        if (level == CommitteeLevel.DISTRICT && (currentUser.getCommitteeLevel() != CommitteeLevel.DIVISIONAL && currentUser.getCommitteeLevel() != CommitteeLevel.CENTRAL)) {
            return false;
        }
        // open the election
        declared = true;
        // clear previous candidates/votes/winners for a fresh election
        candidates.clear();
        votes.clear();
        winners.clear();
        voterRoleVotes.clear();
        return true;
    }

    public boolean vote(Member voter, Role role, Member candidate) {
        if (!declared) {
            return false;
        }
        if (role == null || candidate == null || voter == null) return false;
        if (role == Role.MEMBER) {
            return false;
        }
        List<Member> list = candidates.get(role);
        if (list == null || !list.contains(candidate)) {
            return false;
        }
        //making sure not voting multiple times for same role
        voterRoleVotes.computeIfAbsent(voter, k -> new HashSet<>());
        if (voterRoleVotes.get(voter).contains(role)) {
            return false;
        }
        votes.computeIfAbsent(role, k -> new HashMap<>());
        votes.get(role).merge(candidate, 1, Integer::sum);
        voterRoleVotes.get(voter).add(role);
        return true;
    }

    public void countAndSetWinners() {
        if (!declared) {
            throw new IllegalStateException("Election not active for counting!");
        }
        winners.clear();
        for (Map.Entry<Role, List<Member>> entry : candidates.entrySet()) {
            Role role = entry.getKey();
            List<Member> cands = entry.getValue();
            if (cands == null || cands.isEmpty()) {
                continue;
            }
            Map<Member, Integer> roleVotes = votes.get(role);
            Member best = null;
            int bestVotes = -1;
            for (Member m : cands) {
                int v = (roleVotes == null) ? 0 : roleVotes.getOrDefault(m, 0);
                if (v > bestVotes) {
                    best = m;
                    bestVotes = v;
                } else if (v == bestVotes && best != null) {
                    // tie -> keep first encountered (stable)
                }
            }
            if (best != null) {
                CommitteeLevel level = null;
                if (committee instanceof CentralCommittee) {
                    level = CommitteeLevel.CENTRAL;
                } else if (committee instanceof DivisionalCommittee) {
                    level = CommitteeLevel.DIVISIONAL;
                } else if (committee instanceof DistrictCommittee) {
                    level = CommitteeLevel.DISTRICT;
                }
                best.setCommitteeLevel(level);
                best.setRole(role);
                winners.add(best);
            }
        }
    }

    public List<Member> getWinners(){
        return new ArrayList<>(winners);
    }

    public boolean closeElection(Member currentUser){
        CommitteeLevel level = null;
        if (committee instanceof CentralCommittee) {
            level = CommitteeLevel.CENTRAL;
        } else if (committee instanceof DivisionalCommittee) {
            level = CommitteeLevel.DIVISIONAL;
        } else if (committee instanceof DistrictCommittee) {
            level = CommitteeLevel.DISTRICT;
        }
        if (!declared) {
            return false;
        }
        if (currentUser == null) return false;
        if (currentUser.getRole() != Role.ADMIN && currentUser.getRole() != Role.PRESIDENT) {
            return false;
        }
        if (level == null) {
            return false;
        }
        if (level == CommitteeLevel.CENTRAL && currentUser.getCommitteeLevel() != CommitteeLevel.CENTRAL) {
            return false;
        }
        if (level == CommitteeLevel.DIVISIONAL && currentUser.getCommitteeLevel() != CommitteeLevel.CENTRAL) {
            return false;
        }
        if (level == CommitteeLevel.DISTRICT && (currentUser.getCommitteeLevel() != CommitteeLevel.DIVISIONAL && currentUser.getCommitteeLevel() != CommitteeLevel.CENTRAL)) {
            return false;
        }
        // count winners and add them to committee
        winners.clear();
        countAndSetWinners();
        if (committee instanceof CentralCommittee) {
            for (Member leader : winners) {
                ((CentralCommittee) committee).addLeader(leader);
            }
        } else if (committee instanceof DivisionalCommittee) {
            for (Member leader : winners) {
                ((DivisionalCommittee) committee).addLeader(leader);
            }
        } else if (committee instanceof DistrictCommittee) {
            for (Member leader : winners) {
                ((DistrictCommittee) committee).addLeader(leader);
            }
        }
        declared = false;
        return true;
    }

    public Map<Role, List<Member>> getAllCandidates() {
        Map<Role, List<Member>> snapshot = new EnumMap<>(Role.class);
        for (Map.Entry<Role, List<Member>> entry : candidates.entrySet()) {
            snapshot.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return snapshot;
    }

    public boolean isDeclared() {
        return declared;
    }
}
