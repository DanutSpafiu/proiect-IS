package spafi.springframework.magazinonline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request body for a buyer submitting an offer on a negotiable product.
 * The product is identified by the path (its public reference), so only the
 * proposed price is supplied here.
 */
public record OfferRequest(
        @NotNull(message = "Proposed price is required")
        @Positive(message = "Proposed price must be positive")
        Double proposedPrice
) {
}
