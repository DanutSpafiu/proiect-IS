package spafi.springframework.magazinonline.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import spafi.springframework.magazinonline.model.SaleHistory;
import spafi.springframework.magazinonline.model.User;

@Repository
public interface SaleHistoryRepository extends JpaRepository<SaleHistory, Long> {

    List<SaleHistory> findByBuyer(User buyer);

    List<SaleHistory> findBySeller(User seller);
}
