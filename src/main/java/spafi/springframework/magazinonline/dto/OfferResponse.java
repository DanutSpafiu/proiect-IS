package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;

/**
 * View of an offer. {@code reference} (the offer's public UUID) is {@code null} for an
 * offer that was rejected on submission for being below the minimum price — such an
 * offer is never stored, so it has no handle.
 */
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

    /** An offer that was rejected immediately and never persisted (no reference). */
    public static OfferResponse rejectedUnsaved(String productReference, String buyerEmail, Double proposedPrice) {
        return new OfferResponse(null, productReference, buyerEmail, proposedPrice, OfferStatus.REJECTED);
    }
}
