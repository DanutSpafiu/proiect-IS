package spafi.springframework.magazinonline.dto;

import java.time.LocalDateTime;
import spafi.springframework.magazinonline.model.SaleHistory;

/** Public view of a completed sale. */
public record SaleHistoryResponse(
        String productName,
        String productDescription,
        Double finalPrice,
        String buyerEmail,
        String sellerEmail,
        LocalDateTime soldAt
) {
    public static SaleHistoryResponse fromSaleHistory(SaleHistory saleHistory) {
        return new SaleHistoryResponse(
                saleHistory.getProductName(),
                saleHistory.getProductDescription(),
                saleHistory.getFinalPrice(),
                saleHistory.getBuyer().getEmail(),
                saleHistory.getSeller().getEmail(),
                saleHistory.getSoldAt());
    }
}
