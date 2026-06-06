package spafi.springframework.magazinonline.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spafi.springframework.magazinonline.model.Offer;
import spafi.springframework.magazinonline.model.OfferStatus;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.User;

@Repository
public interface OfferRepository extends JpaRepository<Offer, Long> {

    List<Offer> findByProduct(Product product);

    List<Offer> findByProductAndStatus(Product product, OfferStatus status);

    List<Offer> findByBuyer(User buyer);

    Optional<Offer> findByProductAndBuyerAndStatus(Product product, User buyer, OfferStatus status);

    void deleteByProduct(Product product);
}