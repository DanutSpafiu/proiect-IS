//xia


package spafi.springframework.magazinonline.service;

import java.time.LocalDateTime;
import java.util.List;
import spafi.springframework.magazinonline.dto.OfferRequest;
import spafi.springframework.magazinonline.dto.OfferResponse;
import spafi.springframework.magazinonline.dto.ProductCreateRequest;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.dto.SaleHistoryResponse;
import spafi.springframework.magazinonline.exception.InvalidOperationException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.SaleHistory;
import spafi.springframework.magazinonline.model.SaleType;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.OfferRepository;
import spafi.springframework.magazinonline.repository.ProductRepository;
import spafi.springframework.magazinonline.repository.SaleHistoryRepository;
import spafi.springframework.magazinonline.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OfferRepository offerRepository;
    private final SaleHistoryRepository saleHistoryRepository;

    public ProductService(ProductRepository productRepository,
                          UserRepository userRepository,
                          OfferRepository offerRepository,
                          SaleHistoryRepository saleHistoryRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.offerRepository = offerRepository;
        this.saleHistoryRepository = saleHistoryRepository;
    }

    public ProductResponse addProduct(ProductCreateRequest request, String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found"));

        if (seller.getRole() != Role.SELLER) {
            throw new InvalidOperationException("Only sellers can add products");
        }

        if (!seller.isApproved()) {
            throw new InvalidOperationException("Seller is not approved yet");
        }

        if (!seller.isActive()) {
            throw new InvalidOperationException("Seller account is not active");
        }

        if (request.getSaleType() == SaleType.NEGOTIABLE && request.getMinimumPrice() == null) {
            throw new InvalidOperationException("Minimum price is required for negotiable products");
        }

        if (request.getSaleType() == SaleType.FIXED_PRICE) {
            request.setMinimumPrice(null);
        }

        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setDescription(request.getDescription());
        product.setSaleType(request.getSaleType());
        product.setMinimumPrice(request.getMinimumPrice());
        product.setSeller(seller);

        Product savedProduct = productRepository.save(product);

        return ProductResponse.fromProduct(savedProduct);
    }

    public List<ProductResponse> listAvailableProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::fromProduct)
                .toList();
    }

    public OfferResponse submitOffer(Long productId, String buyerEmail, OfferRequest request) {
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        if (buyer.getRole() != Role.BUYER) {
            throw new InvalidOperationException("Only buyers can submit offers");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getSaleType() != SaleType.NEGOTIABLE) {
            throw new InvalidOperationException("Offers are only allowed on negotiable products");
        }

        if (request.getProposedPrice() < product.getMinimumPrice()) {
            throw new InvalidOperationException("Offer rejected: proposed price is below the product minimum");
        }

        Offer offer = Offer.builder()
                .product(product)
                .buyer(buyer)
                .proposedPrice(request.getProposedPrice())
                .status(OfferStatus.PENDING)
                .build();

        Offer saved = offerRepository.save(offer);
        return OfferResponse.fromOffer(saved);
    }

    public List<OfferResponse> listOffersForProduct(Long productId, String sellerEmail) {
        Product product = findProductOwnedBySeller(productId, sellerEmail);
        return offerRepository.findByProduct(product).stream()
                .map(OfferResponse::fromOffer)
                .toList();
    }

    public OfferResponse approveOffer(Long offerId, String sellerEmail) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        validateSellerOwnsProduct(offer.getProduct(), sellerEmail);

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidOperationException("Only pending offers can be approved");
        }

        offer.setStatus(OfferStatus.APPROVED);
        return OfferResponse.fromOffer(offerRepository.save(offer));
    }

    public OfferResponse rejectOffer(Long offerId, String sellerEmail) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found"));

        validateSellerOwnsProduct(offer.getProduct(), sellerEmail);

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new InvalidOperationException("Only pending offers can be rejected");
        }

        offer.setStatus(OfferStatus.REJECTED);
        return OfferResponse.fromOffer(offerRepository.save(offer));
    }

    public SaleHistoryResponse purchaseProduct(Long productId, String buyerEmail) {
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        if (buyer.getRole() != Role.BUYER) {
            throw new InvalidOperationException("Only buyers can purchase products");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        double finalPrice;
        if (product.getSaleType() == SaleType.FIXED_PRICE) {
            finalPrice = product.getPrice();
        } else {
            Offer approvedOffer = offerRepository.findByProductAndBuyerAndStatus(product, buyer, OfferStatus.APPROVED)
                    .orElseThrow(() -> new InvalidOperationException("No approved offer found for this product"));
            finalPrice = approvedOffer.getProposedPrice();
        }

        SaleHistory saleHistory = SaleHistory.builder()
                .productName(product.getName())
                .productDescription(product.getDescription())
                .finalPrice(finalPrice)
                .buyer(buyer)
                .seller(product.getSeller())
                .soldAt(LocalDateTime.now())
                .build();

        saleHistoryRepository.save(saleHistory);
        offerRepository.deleteByProduct(product);
        productRepository.delete(product);

        return SaleHistoryResponse.fromSaleHistory(saleHistory);
    }

    public List<OfferResponse> listOffersByBuyer(String buyerEmail) {
        User buyer = userRepository.findByEmail(buyerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Buyer not found"));

        if (buyer.getRole() != Role.BUYER) {
            throw new InvalidOperationException("Only buyers can view their offers");
        }

        return offerRepository.findByBuyer(buyer).stream()
                .map(OfferResponse::fromOffer)
                .toList();
    }

    private Product findProductOwnedBySeller(Long productId, String sellerEmail) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        validateSellerOwnsProduct(product, sellerEmail);
        return product;
    }

    private void validateSellerOwnsProduct(Product product, String sellerEmail) {
        if (!product.getSeller().getEmail().equals(sellerEmail)) {
            throw new InvalidOperationException("Seller does not own this product");
        }
    }
}