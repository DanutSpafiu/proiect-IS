package spafi.springframework.magazinonline.controller;

import jakarta.validation.Valid;
import java.security.Principal;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import spafi.springframework.magazinonline.dto.RegistrationRequest;
import spafi.springframework.magazinonline.dto.UserResponse;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.repository.UserRepository;
import spafi.springframework.magazinonline.service.AuthService;

/**
 * Public registration endpoints plus an authenticated "who am I" endpoint.
 * Login itself is handled by Spring Security (HTTP Basic) on any protected request.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    /** Buyer self-registration — no approval required. */
    @PostMapping("/register/buyer")
    public ResponseEntity<UserResponse> registerBuyer(@Valid @RequestBody RegistrationRequest request) {
        UserResponse response = UserResponse.from(authService.registerBuyer(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Seller account request — created pending admin approval. */
    @PostMapping("/register/seller")
    public ResponseEntity<UserResponse> registerSeller(@Valid @RequestBody RegistrationRequest request) {
        UserResponse response = UserResponse.from(authService.registerSeller(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /** Details of the currently authenticated account. */
    @GetMapping("/me")
    public ResponseEntity<UserResponse> currentUser(Principal principal) {
        UserResponse response = userRepository.findByEmail(principal.getName())
                .map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
        return ResponseEntity.ok(response);
    }
}
