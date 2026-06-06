package spafi.springframework.magazinonline.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.repository.ProductRepository;

@RestController
@RequestMapping("/api/products")
public class PublicProductController {

    private final ProductRepository productRepository;

    public PublicProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts() {
        List<ProductResponse> products = productRepository.findAll().stream()
                .map(ProductResponse::fromProduct)
                .toList();
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return ResponseEntity.ok(ProductResponse.fromProduct(product));
    }
}