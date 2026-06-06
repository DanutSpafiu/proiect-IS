package spafi.springframework.magazinonline.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.dto.SaleHistoryResponse;
import spafi.springframework.magazinonline.exception.InvalidOperationException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.SaleHistory;
import spafi.springframework.magazinonline.model.SaleType;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.OfferRepository;
import spafi.springframework.magazinonline.repository.ProductRepository;
import spafi.springframework.magazinonline.repository.SaleHistoryRepository;

/**
 * Completes purchases. A fixed-price product sells at its listed price; a negotiable
 * product can only be bought by a buyer whose offer the seller already approved, at the
 * approved price. On sale a {@link SaleHistory} snapshot is written and the product and
 * all its offers are deleted.
 */
@Service
public class PurchaseService {

    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final SaleHistoryRepository saleHistoryRepository;
    private final AccountService accountService;

    public PurchaseService(
            ProductRepository productRepository,
            OfferRepository offerRepository,
            SaleHistoryRepository saleHistoryRepository,
            AccountService accountService) {
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.saleHistoryRepository = saleHistoryRepository;
        this.accountService = accountService;
    }

    @Transactional
    public SaleHistoryResponse purchase(String buyerEmail, String productReference) {
        User buyer = accountService.requireBuyer(buyerEmail);
        Product product = requireProduct(productReference);

        double finalPrice = resolveFinalPrice(product, buyer);

        SaleHistory sale = SaleHistory.builder()
                .productName(product.getName())
                .productDescription(product.getDescription())
                .finalPrice(finalPrice)
                .buyer(buyer)
                .seller(product.getSeller())
                .soldAt(LocalDateTime.now())
                .build();
        SaleHistory saved = saleHistoryRepository.save(sale);

        // The listing and any offers on it are removed once the product is sold.
        offerRepository.deleteByProduct(product);
        productRepository.delete(product);

        return SaleHistoryResponse.from(saved);
    }

    /** A buyer's completed purchases. */
    @Transactional(readOnly = true)
    public List<SaleHistoryResponse> listPurchases(String buyerEmail) {
        User buyer = accountService.requireBuyer(buyerEmail);
        return saleHistoryRepository.findByBuyer(buyer).stream()
                .map(SaleHistoryResponse::from)
                .toList();
    }

    private double resolveFinalPrice(Product product, User buyer) {
        if (product.getSaleType() == SaleType.FIXED_PRICE) {
            return product.getPrice();
        }
        // Negotiable: require an approved offer from this buyer; sell at the agreed price.
        List<Offer> approved = offerRepository.findByProductAndBuyerAndStatus(
                product, buyer, OfferStatus.APPROVED);
        if (approved.isEmpty()) {
            throw new InvalidOperationException(
                    "This product can only be purchased after the seller approves your offer");
        }
        return approved.get(0).getProposedPrice();
    }

    private Product requireProduct(String reference) {
        UUID publicId = References.parse(reference, "product");
        return productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No product found with reference: " + reference));
    }
}
