package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;

/** Public representation of an offer on a negotiable product. */
public record OfferResponse(
        Long offerId,
        Long productId,
        String buyerEmail,
        Double proposedPrice,
        OfferStatus status
) {
    public static OfferResponse fromOffer(Offer offer) {
        return new OfferResponse(
                offer.getId(),
                offer.getProduct().getId(),
                offer.getBuyer().getEmail(),
                offer.getProposedPrice(),
                offer.getStatus());
    }
}
