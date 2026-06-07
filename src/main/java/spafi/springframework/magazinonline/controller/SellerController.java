package spafi.springframework.magazinonline.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spafi.springframework.magazinonline.dto.OfferResponse;
import spafi.springframework.magazinonline.dto.ProductCreateRequest;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.service.OfferService;
import spafi.springframework.magazinonline.service.ProductService;

@RestController
@RequestMapping("/api/seller")
public class SellerController {

    private final ProductService productService;
    private final OfferService offerService;

    public SellerController(ProductService productService, OfferService offerService) {
        this.productService = productService;
        this.offerService = offerService;
    }

    @PostMapping("/products")
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request, Principal principal) {
        ProductResponse response = productService.createProduct(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/products")
    public ResponseEntity<List<ProductResponse>> listOwnProducts(Principal principal) {
        return ResponseEntity.ok(productService.listOwnProducts(principal.getName()));
    }

    @DeleteMapping("/products/{reference}")
    public ResponseEntity<Void> cancelListing(@PathVariable String reference, Principal principal) {
        productService.cancelListing(principal.getName(), reference);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/offers")
    public ResponseEntity<List<OfferResponse>> listOffers(Principal principal) {
        return ResponseEntity.ok(offerService.listOffersForSeller(principal.getName()));
    }

    @PostMapping("/offers/{reference}/approve")
    public ResponseEntity<OfferResponse> approveOffer(
            @PathVariable String reference, Principal principal) {
        return ResponseEntity.ok(offerService.approveOffer(principal.getName(), reference));
    }

    @PostMapping("/offers/{reference}/reject")
    public ResponseEntity<OfferResponse> rejectOffer(
            @PathVariable String reference, Principal principal) {
        return ResponseEntity.ok(offerService.rejectOffer(principal.getName(), reference));
    }
}
