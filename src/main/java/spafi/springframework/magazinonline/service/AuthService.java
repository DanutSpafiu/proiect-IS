package spafi.springframework.magazinonline.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.dto.RegistrationRequest;
import spafi.springframework.magazinonline.exception.EmailAlreadyExistsException;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.UserRepository;

/**
 * Handles account creation for the two self-service flows: buyer registration
 * (immediately usable) and seller account requests (pending admin approval).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Buyers are active and approved immediately — no admin gate. */
    @Transactional
    public User registerBuyer(RegistrationRequest request) {
        return createUser(request, Role.BUYER, true);
    }

    /**
     * Sellers are created active but <em>not</em> approved; they may log in but
     * cannot list products until an admin approves the account.
     */
    @Transactional
    public User registerSeller(RegistrationRequest request) {
        return createUser(request, Role.SELLER, false);
    }

    private User createUser(RegistrationRequest request, Role role, boolean approved) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = User.builder()
                .email(email)
                .password(passwordEncoder.encode(request.password()))
                .role(role)
                .approved(approved)
                .active(true)
                .build();
        return userRepository.save(user);
    }
}