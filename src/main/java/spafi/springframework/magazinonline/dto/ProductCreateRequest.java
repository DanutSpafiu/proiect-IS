package spafi.springframework.magazinonline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import spafi.springframework.magazinonline.model.SaleType;

public record ProductCreateRequest(
        @NotBlank(message = "Name is required")
        String name,

        @NotNull(message = "Price is required")
        @Positive(message = "Price must be positive")
        Double price,

        String description,

        @NotNull(message = "Sale type is required")
        SaleType saleType,

        @Positive(message = "Minimum price must be positive")
        Double minimumPrice
) {
}
