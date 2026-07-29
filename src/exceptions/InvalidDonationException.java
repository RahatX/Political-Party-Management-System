package exceptions;

public class InvalidDonationException extends Exception {
    private static final long serialVersionUID = 1L;

    public InvalidDonationException(String msg) {
        super(msg);
    }
}
