package spafi.springframework.magazinonline.controller;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import spafi.springframework.magazinonline.dto.OfferRequest;
import spafi.springframework.magazinonline.dto.OfferResponse;
import spafi.springframework.magazinonline.dto.SaleHistoryResponse;
import spafi.springframework.magazinonline.service.ProductService;

@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final ProductService productService;

    public BuyerController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/offers")
    public ResponseEntity<List<OfferResponse>> listMyOffers(Authentication authentication) {
        String buyerEmail = authentication.getName();
        return ResponseEntity.ok(productService.listOffersByBuyer(buyerEmail));
    }

    @PostMapping("/products/{productId}/offer")
    public ResponseEntity<OfferResponse> submitOffer(
            @PathVariable Long productId,
            @Valid @RequestBody OfferRequest request,
            Authentication authentication
    ) {
        String buyerEmail = authentication.getName();
        OfferResponse response = productService.submitOffer(productId, buyerEmail, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/products/{productId}/purchase")
    public ResponseEntity<SaleHistoryResponse> purchaseProduct(
            @PathVariable Long productId,
            Authentication authentication
    ) {
        String buyerEmail = authentication.getName();
        SaleHistoryResponse response = productService.purchaseProduct(productId, buyerEmail);
        return ResponseEntity.ok(response);
    }
}