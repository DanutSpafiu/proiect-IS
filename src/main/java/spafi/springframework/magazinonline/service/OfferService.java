package spafi.springframework.magazinonline.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.model.*;
import spafi.springframework.magazinonline.repository.*;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final SaleHistoryRepository saleHistoryRepository;

    public OfferService(OfferRepository offerRepository, ProductRepository productRepository,
                        UserRepository userRepository, SaleHistoryRepository saleHistoryRepository) {
        this.offerRepository = offerRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.saleHistoryRepository = saleHistoryRepository;
    }

    // 1. CUMPARATORUL TRIMITE OFERTA
    public Offer trimiteOferta(Long buyerId, Long productId, Double proposedPrice) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Produsul nu a fost gasit."));

        if (product.getSaleType() != SaleType.NEGOTIABLE) {
            throw new RuntimeException("Acest produs nu este negociabil.");
        }

        if (proposedPrice < product.getMinimumPrice()) {
            throw new RuntimeException("Oferta respinsa: Pretul propus este sub pretul minim.");
        }

        User buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> new RuntimeException("Cumparatorul nu a fost gasit."));

        Offer newOffer = Offer.builder()
                .product(product)
                .buyer(buyer)
                .proposedPrice(proposedPrice)
                .status(OfferStatus.PENDING)
                .build();

        return offerRepository.save(newOffer);
    }

    // 2. VANZATORUL APROBA OFERTA
    public void aprobaOferta(Long sellerId, Long offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Oferta nu a fost gasita."));

        // Verificam daca cel care aproba este chiar vanzatorul produsului
        if (!offer.getProduct().getSeller().getId().equals(sellerId)) {
            throw new RuntimeException("Doar vanzatorul produsului poate aproba oferta.");
        }

        offer.setStatus(OfferStatus.APPROVED);
        offerRepository.save(offer);
    }

    // 3. CUMPARATORUL CUMPARA PRODUSUL (Istoric + Stergere)
    @Transactional
    public void cumparaProdusNegociat(Long buyerId, Long offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new RuntimeException("Oferta nu a fost gasita."));

        if (!offer.getBuyer().getId().equals(buyerId)) {
            throw new RuntimeException("Doar cumparatorul care a facut oferta poate cumpara produsul.");
        }

        if (offer.getStatus() != OfferStatus.APPROVED) {
            throw new RuntimeException("Oferta nu a fost aprobata de vanzator inca.");
        }

        Product product = offer.getProduct();

        // A. Inregistram vanzarea in istoric
        SaleHistory istoric = SaleHistory.builder()
                .productName(product.getName())
                .productDescription(product.getDescription())
                .finalPrice(offer.getProposedPrice())
                .buyer(offer.getBuyer())
                .seller(product.getSeller())
                .soldAt(LocalDateTime.now())
                .build();
        saleHistoryRepository.save(istoric);

        // B. Stergem produsul si ofertele asociate
        // Baza de date ne obliga sa stergem intai ofertele, apoi produsul (regula Foreign Key)
        List<Offer> oferteAsociate = offerRepository.findByProduct(product);
        offerRepository.deleteAll(oferteAsociate);
        productRepository.delete(product);
    }
}