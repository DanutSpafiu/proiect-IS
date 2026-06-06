//xia


package spafi.springframework.magazinonline.service;

import org.springframework.stereotype.Service;
import spafi.springframework.magazinonline.dto.ProductCreateRequest;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.SaleType;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.ProductRepository;
import spafi.springframework.magazinonline.repository.UserRepository;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public ProductResponse addProduct(ProductCreateRequest request, String sellerEmail) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new RuntimeException("Seller not found"));

        if (seller.getRole() != Role.SELLER) {
            throw new RuntimeException("Only sellers can add products");
        }

        if (!seller.isApproved()) {
            throw new RuntimeException("Seller is not approved yet");
        }

        if (!seller.isActive()) {
            throw new RuntimeException("Seller account is not active");
        }

        if (request.getSaleType() == SaleType.NEGOTIABLE && request.getMinimumPrice() == null) {
            throw new RuntimeException("Minimum price is required for negotiable products");
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
}