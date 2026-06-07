package spafi.springframework.magazinonline.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.exception.InvalidOperationException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;

    public AdminService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> listSellers() {
        return userRepository.findByRole(Role.SELLER);
    }

    @Transactional
    public User approveSeller(String email) {
        User seller = requireSeller(email);
        seller.setApproved(true);
        return userRepository.save(seller);
    }

    @Transactional
    public User deactivateSeller(String email) {
        User seller = requireSeller(email);
        seller.setActive(false);
        return userRepository.save(seller);
    }

    private User requireSeller(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No seller found with email: " + email));
        if (user.getRole() != Role.SELLER) {
            throw new InvalidOperationException("Account '" + email + "' is not a seller");
        }
        return user;
    }
}
