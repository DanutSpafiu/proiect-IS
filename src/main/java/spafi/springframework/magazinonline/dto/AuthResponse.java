package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Role;

/**
 * Returned on a successful login: the signed JWT the client must send back as
 * {@code Authorization: Bearer <token>}, plus a little context about the account.
 */
public record AuthResponse(
        String token,
        String email,
        Role role
) {
}
