package service;

public final class SystemStats {
    private final int approvedMembers;
    private final int pendingApplications;
    private final int leaders;
    private final int activeElections;
    private final double totalDonations;

    public SystemStats(
            int approvedMembers,
            int pendingApplications,
            int leaders,
            int activeElections,
            double totalDonations) {
        this.approvedMembers = approvedMembers;
        this.pendingApplications = pendingApplications;
        this.leaders = leaders;
        this.activeElections = activeElections;
        this.totalDonations = totalDonations;
    }

    public int getApprovedMembers() {
        return approvedMembers;
    }

    public int getPendingApplications() {
        return pendingApplications;
    }

    public int getLeaders() {
        return leaders;
    }

    public int getActiveElections() {
        return activeElections;
    }

    public double getTotalDonations() {
        return totalDonations;
    }
}
