package spafi.springframework.magazinonline.dto;

import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;

/**
 * Safe view of a user account. Never exposes the database id or the password hash.
 */
public record UserResponse(
        String email,
        Role role,
        boolean approved,
        boolean active
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getEmail(),
                user.getRole(),
                user.isApproved(),
                user.isActive());
    }
}