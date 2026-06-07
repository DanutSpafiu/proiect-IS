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

@RestController
@RequestMapping("/api/buyer")
public class BuyerController {

    private final PurchaseService purchaseService;
    private final OfferService offerService;

    public BuyerController(PurchaseService purchaseService, OfferService offerService) {
        this.purchaseService = purchaseService;
        this.offerService = offerService;
    }

    @PostMapping("/products/{reference}/purchase")
    public ResponseEntity<SaleHistoryResponse> purchase(
            @PathVariable String reference, Principal principal) {
        SaleHistoryResponse response = purchaseService.purchase(principal.getName(), reference);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/products/{reference}/offers")
    public ResponseEntity<OfferResponse> submitOffer(
            @PathVariable String reference,
            @Valid @RequestBody OfferRequest request,
            Principal principal) {
        return ResponseEntity.ok(offerService.submitOffer(principal.getName(), reference, request));
    }

    @GetMapping("/offers")
    public ResponseEntity<List<OfferResponse>> listOffers(Principal principal) {
        return ResponseEntity.ok(offerService.listOffersForBuyer(principal.getName()));
    }

    @GetMapping("/purchases")
    public ResponseEntity<List<SaleHistoryResponse>> listPurchases(Principal principal) {
        return ResponseEntity.ok(purchaseService.listPurchases(principal.getName()));
    }
}
