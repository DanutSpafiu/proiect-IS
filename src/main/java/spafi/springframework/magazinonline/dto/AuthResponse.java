package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Role;

public record AuthResponse(
        String token,
        String email,
        Role role
) {
}
