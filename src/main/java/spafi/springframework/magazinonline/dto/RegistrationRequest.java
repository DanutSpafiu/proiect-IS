package spafi.springframework.magazinonline.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistrationRequest(
        @NotBlank(message = "Email is required")
        @Email(message = "A valid email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 4, message = "Password must be at least 4 characters")
        String password
) {
}
