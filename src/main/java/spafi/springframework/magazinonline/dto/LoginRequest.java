package spafi.springframework.magazinonline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credentials submitted to {@code POST /api/auth/login}.
 */
public record LoginRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        String email,

        @NotBlank(message = "Password is required")
        String password
) {
}
