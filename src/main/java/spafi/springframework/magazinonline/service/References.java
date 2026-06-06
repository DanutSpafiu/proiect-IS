package spafi.springframework.magazinonline.service;

import java.util.UUID;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;

/**
 * Helpers for turning a public reference string from a request into a {@link UUID}.
 * A malformed reference is treated as "not found" rather than a server error.
 */
final class References {

    private References() {
    }

    static UUID parse(String reference, String entityName) {
        try {
            return UUID.fromString(reference);
        } catch (IllegalArgumentException ex) {
            throw new ResourceNotFoundException("No " + entityName + " found with reference: " + reference);
        }
    }
}
