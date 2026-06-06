package spafi.springframework.magazinonline.service;

import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.dto.ProductCreateRequest;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.exception.InvalidOperationException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.SaleType;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.OfferRepository;
import spafi.springframework.magazinonline.repository.ProductRepository;

/**
 * Listing lifecycle: approved sellers create and cancel listings; everyone can browse.
 */
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final OfferRepository offerRepository;
    private final AccountService accountService;

    public ProductService(
            ProductRepository productRepository,
            OfferRepository offerRepository,
            AccountService accountService) {
        this.productRepository = productRepository;
        this.offerRepository = offerRepository;
        this.accountService = accountService;
    }

    /** List a product for sale (approved sellers only). */
    @Transactional
    public ProductResponse createProduct(String sellerEmail, ProductCreateRequest request) {
        User seller = accountService.requireApprovedSeller(sellerEmail);

        Double minimumPrice = null;
        if (request.saleType() == SaleType.NEGOTIABLE) {
            if (request.minimumPrice() == null) {
                throw new InvalidOperationException("A negotiable product requires a minimum price");
            }
            minimumPrice = request.minimumPrice();
        } else if (request.minimumPrice() != null) {
            throw new InvalidOperationException("A fixed-price product must not have a minimum price");
        }

        Product product = Product.builder()
                .publicId(UUID.randomUUID())
                .name(request.name())
                .price(request.price())
                .description(request.description())
                .saleType(request.saleType())
                .minimumPrice(minimumPrice)
                .seller(seller)
                .build();
        return ProductResponse.from(productRepository.save(product));
    }

    /** Every available product, for the public/buyer catalogue. */
    @Transactional(readOnly = true)
    public List<ProductResponse> listAvailable() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getByReference(String reference) {
        return ProductResponse.from(requireProduct(reference));
    }

    /** A seller's own listings. */
    @Transactional(readOnly = true)
    public List<ProductResponse> listOwnProducts(String sellerEmail) {
        User seller = accountService.requireApprovedSeller(sellerEmail);
        return productRepository.findBySeller(seller).stream()
                .map(ProductResponse::from)
                .toList();
    }

    /** Cancel (remove) one of the seller's own listings, along with any offers on it. */
    @Transactional
    public void cancelListing(String sellerEmail, String reference) {
        User seller = accountService.requireApprovedSeller(sellerEmail);
        Product product = requireProduct(reference);
        if (!product.getSeller().getId().equals(seller.getId())) {
            throw new AccessDeniedException("You can only cancel your own listings");
        }
        offerRepository.deleteByProduct(product);
        productRepository.delete(product);
    }

    private Product requireProduct(String reference) {
        UUID publicId = References.parse(reference, "product");
        return productRepository.findByPublicId(publicId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No product found with reference: " + reference));
    }
}
