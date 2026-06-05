package spafi.springframework.magazinonline.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.UserRepository;

/**
 * Seeds the single hardcoded administrator account on startup if it does not yet
 * exist: {@code admin@email.com} / {@code admin}.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_EMAIL = "admin@email.com";
    private static final String ADMIN_PASSWORD = "admin";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(ADMIN_EMAIL)) {
            return;
        }
        User admin = User.builder()
                .email(ADMIN_EMAIL)
                .password(passwordEncoder.encode(ADMIN_PASSWORD))
                .role(Role.ADMIN)
                .approved(true)
                .active(true)
                .build();
        userRepository.save(admin);
        log.info("Seeded administrator account: {}", ADMIN_EMAIL);
    }
}