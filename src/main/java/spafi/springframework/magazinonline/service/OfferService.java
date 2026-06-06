package spafi.springframework.magazinonline.service;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.dto.OfferRequest;
import spafi.springframework.magazinonline.dto.OfferResponse;
import spafi.springframework.magazinonline.exception.InvalidOperationException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.SaleType;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.OfferRepository;
import spafi.springframework.magazinonline.repository.ProductRepository;

/**
 * Offer lifecycle for negotiable products: buyers submit, sellers approve or reject.
 */
@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final AccountService accountService;

    public OfferService(
            OfferRepository offerRepository,
            ProductRepository productRepository,
            AccountService accountService) {
        this.offerRepository = offerRepository;
        this.productRepository = productRepository;
        this.accountService = accountService;
    }

    /**
     * Submit an offer on a negotiable product. An offer below the product's minimum
     * price is rejected immediately and <em>never stored</em> (returned with a null
     * reference); otherwise it is saved as {@code PENDING} for the seller to review.
     */
    @Transactional
    public OfferResponse submitOffer(String buyerEmail, String productReference, OfferRequest request) {
        User buyer = accountService.requireBuyer(buyerEmail);
        Product product = requireProduct(productReference);

        if (product.getSaleType() != SaleType.NEGOTIABLE) {
            throw new InvalidOperationException("Offers are only allowed on negotiable products");
        }

        if (request.proposedPrice() < product.getMinimumPrice()) {
            // Silent rejection: not persisted, not shown anywhere.
            return OfferResponse.rejectedUnsaved(productReference, buyer.getEmail(), request.proposedPrice());
        }

        Offer offer = Offer.builder()
                .publicId(UUID.randomUUID())
                .product(product)
                .buyer(buyer)
                .proposedPrice(request.proposedPrice())
                .status(OfferStatus.PENDING)
                .build();
        return OfferResponse.from(offerRepository.save(offer));
    }

    @Transactional
    public OfferResponse approveOffer(String sellerEmail, String offerReference) {
        return decide(sellerEmail, offerReference, OfferStatus.APPROVED);
    }

    @Transactional
    public OfferResponse rejectOffer(String sellerEmail, String offerReference) {
        return decide(sellerEmail, offerReference, OfferStatus.REJECTED);
    }

    /** Offers across all of the seller's products. */
    @Transactional(readOnly = true)
    public List<OfferResponse> listOffersForSeller(String sellerEmail) {
        User seller = accountService.requireApprovedSeller(sellerEmail);
        return offerRepository.findByProduct_Seller(seller).stream()
                .map(OfferResponse::from)
                .toList();
    }

    /** A buyer's own offers and their statuses. */
    @Transactional(readOnly = true)
    public List<OfferResponse> listOffersForBuyer(String buyerEmail) {
        User buyer = accountService.requireBuyer(buyerEmail);
        return offerRepository.findByBuyer(buyer).stream()
                .map(OfferResponse::from)
                .toList();
    }

    private OfferResponse decide(String sellerEmail, String offerReference, OfferStatus newStatus) {
        User seller = accountService.requireApprovedSeller(sellerEmail);
        Offer offer = requireOffer(offerReference);
        if (!offer.getProduct().getSeller().getId().equals(seller.getId())) {
            throw new AccessDeniedException("You can only decide offers on your own products");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidOperationException(
                    "Offer has already been " + offer.getStatus().name().toLowerCase());
        }
        offer.setStatus(newStatus);
        return OfferResponse.from(offerRepository.save(offer));
    }

    private Product requireProduct(String reference) {
        UUID publicId = References.parse(reference, "product");
        return productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No product found with reference: " + reference));
    }

    private Offer requireOffer(String reference) {
        UUID publicId = References.parse(reference, "offer");
        return offerRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No offer found with reference: " + reference));
    }
}
