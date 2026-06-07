package spafi.springframework.magazinonline.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.UserRepository;

@Service
public class AccountService {

    private final UserRepository userRepository;

    public AccountService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));
    }

    public User requireApprovedSeller(String email) {
        User user = getByEmail(email);
        if (user.getRole() != Role.SELLER) {
            throw new AccessDeniedException("Account is not a seller");
        }
        if (!user.isApproved()) {
            throw new AccessDeniedException("Seller account is not yet approved by an administrator");
        }
        return user;
    }

    public User requireBuyer(String email) {
        User user = getByEmail(email);
        if (user.getRole() != Role.BUYER) {
            throw new AccessDeniedException("Account is not a buyer");
        }
        return user;
    }
}
