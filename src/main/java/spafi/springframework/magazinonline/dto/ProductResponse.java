package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.SaleType;

/**
 * Public view of a product. Deliberately omits the internal numeric id and the
 * {@code minimumPrice} (requirements: neither is ever returned). The {@code reference}
 * is the product's public UUID, used to address it in purchase/offer requests.
 */
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
