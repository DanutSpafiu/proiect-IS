package spafi.springframework.magazinonline.exception;

/** Thrown when registering with an email that is already taken. */
public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}