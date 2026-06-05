package spafi.springframework.magazinonline.exception;

/** Thrown when an operation is not valid for the current state (e.g. approving a non-seller). */
public class InvalidOperationException extends RuntimeException {
    public InvalidOperationException(String message) {
        super(message);
    }
}