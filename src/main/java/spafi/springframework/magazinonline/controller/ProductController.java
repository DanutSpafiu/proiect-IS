package spafi.springframework.magazinonline.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.service.ProductService;

/**
 * Public catalogue browsing. Open to everyone (and buyers); responses never include
 * the internal id or the minimum price.
 */
@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /** All available products. */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> listProducts() {
        return ResponseEntity.ok(productService.listAvailable());
    }

    /** A single product by its public reference. */
    @GetMapping("/{reference}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable String reference) {
        return ResponseEntity.ok(productService.getByReference(reference));
    }
}
