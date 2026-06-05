package spafi.springframework.magazinonline.exception;

/** Thrown when a requested entity (e.g. a seller) does not exist. */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}