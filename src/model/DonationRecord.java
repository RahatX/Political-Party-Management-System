package model;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class DonationRecord {
    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    private final Instant timestamp;
    private final String donor;
    private final double amount;

    public DonationRecord(Instant timestamp, String donor, double amount) {
        this.timestamp = timestamp;
        this.donor = donor == null || donor.isBlank() ? "Anonymous" : donor;
        this.amount = amount;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getDonor() {
        return donor;
    }

    public double getAmount() {
        return amount;
    }

    public String getFormattedTimestamp() {
        return DISPLAY_FORMAT.format(timestamp.atZone(ZoneId.systemDefault()));
    }
}
