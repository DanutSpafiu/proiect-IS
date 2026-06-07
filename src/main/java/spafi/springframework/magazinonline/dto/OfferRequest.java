package spafi.springframework.magazinonline.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OfferRequest(
        @NotNull(message = "Proposed price is required")
        @Positive(message = "Proposed price must be positive")
        Double proposedPrice
) {
}
