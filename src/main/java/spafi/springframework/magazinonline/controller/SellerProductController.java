//xia


package spafi.springframework.magazinonline.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import spafi.springframework.magazinonline.dto.OfferResponse;
import spafi.springframework.magazinonline.dto.ProductCreateRequest;
import spafi.springframework.magazinonline.dto.ProductResponse;
import spafi.springframework.magazinonline.service.ProductService;

@RestController
@RequestMapping("/api/seller/products")
public class SellerProductController {

    private final ProductService productService;

    public SellerProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> addProduct(
            @RequestBody ProductCreateRequest request,
            Authentication authentication
    ) {
        String sellerEmail = authentication.getName();

        ProductResponse response = productService.addProduct(request, sellerEmail);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{productId}/offers")
    public ResponseEntity<List<OfferResponse>> listOffers(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        String sellerEmail = authentication.getName();
        List<OfferResponse> offers = productService.listOffersForProduct(productId, sellerEmail);
        return ResponseEntity.ok(offers);
    }

    @PostMapping("/offers/{offerId}/approve")
    public ResponseEntity<OfferResponse> approveOffer(
            @PathVariable Long offerId,
            Authentication authentication
    ) {
        String sellerEmail = authentication.getName();
        OfferResponse response = productService.approveOffer(offerId, sellerEmail);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/offers/{offerId}/reject")
    public ResponseEntity<OfferResponse> rejectOffer(
            @PathVariable Long offerId,
            Authentication authentication
    ) {
        String sellerEmail = authentication.getName();
        OfferResponse response = productService.rejectOffer(offerId, sellerEmail);
        return ResponseEntity.ok(response);
    }
}