package spafi.springframework.magazinonline.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single user table holding every account type, distinguished by {@link #role}
 * (the role-enum approach suggested by the requirements).
 *
 * <p>The {@code approved} and {@code active} flags are only meaningful for sellers:
 * <ul>
 *     <li>{@code approved} — set by the admin; a seller must be approved before listing products.</li>
 *     <li>{@code active}   — sellers start active; a deactivated seller stays in the database
 *     but is rejected at login time.</li>
 * </ul>
 * Admins and buyers are always {@code approved == true} and {@code active == true}.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt-encoded password — never the raw value. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Admin approval flag. Relevant for sellers; always true for admin/buyer. */
    @Column(nullable = false)
    private boolean approved;

    /** Deactivation flag. A deactivated seller cannot log in. */
    @Column(nullable = false)
    private boolean active;
}