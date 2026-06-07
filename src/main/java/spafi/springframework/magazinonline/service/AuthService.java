package spafi.springframework.magazinonline.service;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.dto.AuthResponse;
import spafi.springframework.magazinonline.dto.LoginRequest;
import spafi.springframework.magazinonline.dto.RegistrationRequest;
import spafi.springframework.magazinonline.exception.EmailAlreadyExistsException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.UserRepository;
import spafi.springframework.magazinonline.security.JwtService;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        String token = jwtService.generateToken(user);
        return new AuthResponse(token, user.getEmail(), user.getRole());
    }

    @Transactional
    public User registerBuyer(RegistrationRequest request) {
        return createUser(request, Role.BUYER, true);
    }

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
