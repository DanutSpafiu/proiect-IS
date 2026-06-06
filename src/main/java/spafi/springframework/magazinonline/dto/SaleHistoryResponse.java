package spafi.springframework.magazinonline.dto;

import java.time.LocalDateTime;
import spafi.springframework.magazinonline.model.SaleHistory;

/**
 * View of a completed sale. References buyer and seller by email only.
 */
public record SaleHistoryResponse(
        String productName,
        String productDescription,
        Double finalPrice,
        String buyerEmail,
        String sellerEmail,
        LocalDateTime soldAt
) {
    public static SaleHistoryResponse from(SaleHistory sale) {
        return new SaleHistoryResponse(
                sale.getProductName(),
                sale.getProductDescription(),
                sale.getFinalPrice(),
                sale.getBuyer().getEmail(),
                sale.getSeller().getEmail(),
                sale.getSoldAt());
    }
}
