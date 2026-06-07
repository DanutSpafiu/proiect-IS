package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;

public record OfferResponse(
        String reference,
        String productReference,
        String buyerEmail,
        Double proposedPrice,
        OfferStatus status
) {
    public static OfferResponse from(Offer offer) {
        return new OfferResponse(
                offer.getPublicId().toString(),
                offer.getProduct().getPublicId().toString(),
                offer.getBuyer().getEmail(),
                offer.getProposedPrice(),
                offer.getStatus());
    }

    public static OfferResponse rejectedUnsaved(String productReference, String buyerEmail, Double proposedPrice) {
        return new OfferResponse(null, productReference, buyerEmail, proposedPrice, OfferStatus.REJECTED);
    }
}
