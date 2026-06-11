package spafi.springframework.magazinonline.service;

import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import spafi.springframework.magazinonline.exception.InvalidOperationException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.ConfirmationToken;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.ConfirmationTokenRepository;
import spafi.springframework.magazinonline.repository.UserRepository;

/**
 * Handles the e-mail-confirmation half of the account-creation diagram:
 * issuing the activation token, sending the (simulated) e-mail, and activating
 * the account when the link is opened.
 */
@Service
public class EmailConfirmationService {

    private final ConfirmationTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final SimulatedEmailService emailService;
    private final String activationBaseUrl;
    private final long tokenTtlMinutes;

    public EmailConfirmationService(
            ConfirmationTokenRepository tokenRepository,
            UserRepository userRepository,
            SimulatedEmailService emailService,
            @Value("${app.activation.base-url:http://localhost:8080/api/auth/confirm}") String activationBaseUrl,
            @Value("${app.activation.token-ttl-minutes:1440}") long tokenTtlMinutes) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.activationBaseUrl = activationBaseUrl;
        this.tokenTtlMinutes = tokenTtlMinutes;
    }

    /**
     * Creates a fresh activation token for the user, "sends" the e-mail and
     * returns the activation link (handy for testing without SMTP).
     */
    @Transactional
    public String createAndSendToken(User user) {
        LocalDateTime now = LocalDateTime.now();
        String token = UUID.randomUUID().toString();

        ConfirmationToken confirmationToken = ConfirmationToken.builder()
                .token(token)
                .user(user)
                .createdAt(now)
                .expiresAt(now.plusMinutes(tokenTtlMinutes))
                .build();
        tokenRepository.save(confirmationToken);

        String activationLink = activationBaseUrl + "?token=" + token;
        emailService.sendActivationEmail(user.getEmail(), activationLink);
        return activationLink;
    }

    /**
     * Validates the token and activates the linked account.
     *
     * @throws InvalidOperationException if the token is unknown, already used or expired
     */
    @Transactional
    public void confirm(String token) {
        ConfirmationToken confirmationToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new InvalidOperationException("Invalid confirmation token"));

        if (confirmationToken.getConfirmedAt() != null) {
            throw new InvalidOperationException("This account has already been activated");
        }
        if (confirmationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidOperationException("This confirmation link has expired. Please request a new one.");
        }

        User user = confirmationToken.getUser();
        user.setEmailConfirmed(true);
        confirmationToken.setConfirmedAt(LocalDateTime.now());

        userRepository.save(user);
        tokenRepository.save(confirmationToken);
    }

    /**
     * Issues a new activation link for an account that has not been confirmed yet.
     *
     * @throws ResourceNotFoundException if no account exists for the e-mail
     * @throws InvalidOperationException if the account is already activated
     */
    @Transactional
    public String resend(String email) {
        String normalized = email.trim().toLowerCase();
        User user = userRepository.findByEmail(normalized)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + normalized));

        if (user.isEmailConfirmed()) {
            throw new InvalidOperationException("This account is already activated");
        }
        return createAndSendToken(user);
    }
}
