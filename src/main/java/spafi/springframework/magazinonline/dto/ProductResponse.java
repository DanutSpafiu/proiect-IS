package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.SaleType;

public record ProductResponse(
        String reference,
        String name,
        Double price,
        String sellerEmail,
        String description,
        SaleType saleType
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getPublicId().toString(),
                product.getName(),
                product.getPrice(),
                product.getSeller().getEmail(),
                product.getDescription(),
                product.getSaleType());
    }
}
