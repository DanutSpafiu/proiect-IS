package spafi.springframework.magazinonline.service;

import java.util.UUID;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;

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
