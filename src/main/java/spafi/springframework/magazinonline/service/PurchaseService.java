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

        offerRepository.deleteByProduct(product);
        productRepository.delete(product);

        return SaleHistoryResponse.from(saved);
    }

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
