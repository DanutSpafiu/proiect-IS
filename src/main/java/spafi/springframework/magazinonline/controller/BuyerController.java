package spafi.springframework.magazinonline.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spafi.springframework.magazinonline.dto.OfferRequest;
import spafi.springframework.magazinonline.dto.OfferResponse;
import spafi.springframework.magazinonline.dto.SaleHistoryResponse;
import spafi.springframework.magazinonline.service.OfferService;
import spafi.springframework.magazinonline.service.PurchaseService;

/**
 * Buyer operations: purchasing products, submitting offers, and viewing own
 * offers/purchases. Restricted to ROLE_BUYER by the security config.
 */
@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final PurchaseService purchaseService;
    private final OfferService offerService;

    public BuyerController(PurchaseService purchaseService, OfferService offerService) {
        this.purchaseService = purchaseService;
        this.offerService = offerService;
    }

    /**
     * Purchase a product. Fixed-price products sell at the listed price; negotiable
     * products require an already-approved offer from this buyer.
     */
    @PostMapping("/products/{reference}/purchase")
    public ResponseEntity<SaleHistoryResponse> purchase(
            @PathVariable String reference, Principal principal) {
        SaleHistoryResponse response = purchaseService.purchase(principal.getName(), reference);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Submit an offer on a negotiable product. */
    @PostMapping("/products/{reference}/offers")
    public ResponseEntity<OfferResponse> submitOffer(
            @PathVariable String reference,
            @Valid @RequestBody OfferRequest request,
            Principal principal) {
        return ResponseEntity.ok(offerService.submitOffer(principal.getName(), reference, request));
    }

    /** View the buyer's own offers and their statuses. */
    @GetMapping("/offers")
    public ResponseEntity<List<OfferResponse>> listOffers(Principal principal) {
        return ResponseEntity.ok(offerService.listOffersForBuyer(principal.getName()));
    }

    /** View the buyer's completed purchases. */
    @GetMapping("/purchases")
    public ResponseEntity<List<SaleHistoryResponse>> listPurchases(Principal principal) {
        return ResponseEntity.ok(purchaseService.listPurchases(principal.getName()));
    }
}
