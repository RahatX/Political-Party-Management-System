package exceptions;

public class DuplicateMemberException extends Exception {
    private static final long serialVersionUID = 1L;

    public DuplicateMemberException(String msg) {
        super(msg);
    }
}
