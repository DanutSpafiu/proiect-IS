package spafi.springframework.magazinonline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** Payload used by buyers to submit an offer for a negotiable product. */
public record OfferRequest(
        @NotNull(message = "Proposed price is required")
        @Positive(message = "Proposed price must be greater than zero")
        Double proposedPrice
) {
}
