package spafi.springframework.magazinonline.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spafi.springframework.magazinonline.model.Product;
import spafi.springframework.magazinonline.model.User;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findBySeller(User seller);

    Optional<Product> findByPublicId(UUID publicId);
}