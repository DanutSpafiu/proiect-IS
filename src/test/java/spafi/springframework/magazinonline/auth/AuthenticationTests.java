package spafi.springframework.magazinonline.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import spafi.springframework.magazinonline.dto.AuthResponse;
import spafi.springframework.magazinonline.dto.LoginRequest;
import spafi.springframework.magazinonline.dto.RegistrationRequest;
import spafi.springframework.magazinonline.exception.EmailAlreadyExistsException;
import spafi.springframework.magazinonline.exception.ResourceNotFoundException;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.model.User;
import spafi.springframework.magazinonline.repository.UserRepository;
import spafi.springframework.magazinonline.security.CustomUserDetailsService;
import spafi.springframework.magazinonline.security.JwtService;
import spafi.springframework.magazinonline.service.AuthService;

/**
 * All authentication unit tests for MagazinOnline, grouped by collaborator:
 *
 *   - {@link AuthService}                : login (credential check + JWT issue) and registration.
 *   - {@link JwtService}                 : signing, parsing, validation and expiry of JWTs.
 *   - {@link CustomUserDetailsService}   : loading Spring-Security {@code UserDetails} from the DB.
 *
 * These are fast, isolated tests (Mockito for collaborators, no Spring context).
 */
class AuthenticationTests {

    private static User buyer() {
        return User.builder()
                .id(1L)
                .email("user@example.com")
                .password("ENCODED_PW")
                .role(Role.BUYER)
                .approved(true)
                .active(true)
                .build();
    }

    // ================================================================
    //  AuthService — login
    // ================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("AuthService.login")
    class LoginTests {

        @Mock private UserRepository userRepository;
        @Mock private PasswordEncoder passwordEncoder;
        @Mock private AuthenticationManager authenticationManager;
        @Mock private JwtService jwtService;
        @InjectMocks private AuthService authService;

        @Captor private ArgumentCaptor<Authentication> authCaptor;

        @Test
        @DisplayName("returns a token, email and role when credentials are valid")
        void loginSucceeds() {
            User user = buyer();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("signed.jwt.token");

            AuthResponse response = authService.login(new LoginRequest("user@example.com", "secret"));

            assertThat(response.token()).isEqualTo("signed.jwt.token");
            assertThat(response.email()).isEqualTo("user@example.com");
            assertThat(response.role()).isEqualTo(Role.BUYER);
            verify(authenticationManager).authenticate(any(Authentication.class));
        }

        @Test
        @DisplayName("trims and lower-cases the email before authenticating and looking it up")
        void loginNormalizesEmail() {
            User user = buyer();
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(jwtService.generateToken(user)).thenReturn("token");

            authService.login(new LoginRequest("  USER@Example.COM  ", "secret"));

            verify(authenticationManager).authenticate(authCaptor.capture());
            assertThat(authCaptor.getValue().getPrincipal()).isEqualTo("user@example.com");
            assertThat(authCaptor.getValue().getCredentials()).isEqualTo("secret");
            verify(userRepository).findByEmail("user@example.com");
        }

        @Test
        @DisplayName("propagates the exception and never issues a token when authentication fails")
        void loginWithBadCredentials() {
            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "wrong")))
                    .isInstanceOf(BadCredentialsException.class);

            verify(jwtService, never()).generateToken(any());
            verify(userRepository, never()).findByEmail(any());
        }

        @Test
        @DisplayName("throws ResourceNotFoundException if the authenticated user is missing from the DB")
        void loginWhenUserVanished() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(new LoginRequest("user@example.com", "secret")))
                    .isInstanceOf(ResourceNotFoundException.class);

            verify(jwtService, never()).generateToken(any());
        }
    }

    // ================================================================
    //  AuthService — registration
    // ================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("AuthService.register")
    class RegistrationTests {

        @Mock private UserRepository userRepository;
        @Mock private PasswordEncoder passwordEncoder;
        @Mock private AuthenticationManager authenticationManager;
        @Mock private JwtService jwtService;
        @InjectMocks private AuthService authService;

        @Captor private ArgumentCaptor<User> userCaptor;

        @Test
        @DisplayName("registerBuyer stores an approved, active BUYER with an encoded password")
        void registerBuyer() {
            when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
            when(passwordEncoder.encode("secret")).thenReturn("ENCODED_PW");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.registerBuyer(new RegistrationRequest("buyer@example.com", "secret"));

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getEmail()).isEqualTo("buyer@example.com");
            assertThat(saved.getPassword()).isEqualTo("ENCODED_PW");
            assertThat(saved.getRole()).isEqualTo(Role.BUYER);
            assertThat(saved.isApproved()).isTrue();
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("registerSeller stores an active SELLER that still needs approval")
        void registerSeller() {
            when(userRepository.existsByEmail("seller@example.com")).thenReturn(false);
            when(passwordEncoder.encode("secret")).thenReturn("ENCODED_PW");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.registerSeller(new RegistrationRequest("seller@example.com", "secret"));

            verify(userRepository).save(userCaptor.capture());
            User saved = userCaptor.getValue();
            assertThat(saved.getRole()).isEqualTo(Role.SELLER);
            assertThat(saved.isApproved()).isFalse();
            assertThat(saved.isActive()).isTrue();
        }

        @Test
        @DisplayName("normalizes the email (trim + lower-case) before persisting")
        void registerNormalizesEmail() {
            when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
            when(passwordEncoder.encode(any())).thenReturn("ENCODED_PW");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            authService.registerBuyer(new RegistrationRequest("  Buyer@Example.COM ", "secret"));

            verify(userRepository).save(userCaptor.capture());
            assertThat(userCaptor.getValue().getEmail()).isEqualTo("buyer@example.com");
        }

        @Test
        @DisplayName("rejects a duplicate email and never saves")
        void registerDuplicateEmail() {
            when(userRepository.existsByEmail("buyer@example.com")).thenReturn(true);

            assertThatThrownBy(() -> authService.registerBuyer(
                    new RegistrationRequest("buyer@example.com", "secret")))
                    .isInstanceOf(EmailAlreadyExistsException.class);

            verify(userRepository, never()).save(any());
        }
    }

    // ================================================================
    //  JwtService — token signing / parsing / validation
    // ================================================================
    @Nested
    @DisplayName("JwtService")
    class JwtServiceTests {

        private static final String SECRET =
                "test-secret-key-that-is-definitely-long-enough-for-hs256-123456";

        private JwtService jwtService(long expirationMs) {
            return new JwtService(SECRET, expirationMs);
        }

        @Test
        @DisplayName("issues a token whose subject is the user email and reports it valid")
        void generateAndParse() {
            JwtService jwt = jwtService(60_000);
            String token = jwt.generateToken(buyer());

            assertThat(token).isNotBlank();
            assertThat(jwt.extractEmail(token)).isEqualTo("user@example.com");
            assertThat(jwt.isValid(token)).isTrue();
        }

        @Test
        @DisplayName("rejects a token that was tampered with")
        void rejectsTamperedToken() {
            JwtService jwt = jwtService(60_000);
            String token = jwt.generateToken(buyer());

            String tampered = token.substring(0, token.length() - 2)
                    + (token.endsWith("a") ? "b" : "a") + "X";

            assertThat(jwt.isValid(tampered)).isFalse();
        }

        @Test
        @DisplayName("rejects a token signed with a different secret")
        void rejectsForeignSignature() {
            String foreign = new JwtService(
                    "another-totally-different-secret-key-1234567890-abcdef", 60_000)
                    .generateToken(buyer());

            assertThat(jwtService(60_000).isValid(foreign)).isFalse();
        }

        @Test
        @DisplayName("rejects an expired token")
        void rejectsExpiredToken() {
            JwtService jwt = jwtService(-1_000); // issued already in the past
            String token = jwt.generateToken(buyer());

            assertThat(jwt.isValid(token)).isFalse();
        }

        @Test
        @DisplayName("rejects garbage / non-JWT input")
        void rejectsGarbage() {
            JwtService jwt = jwtService(60_000);

            assertThat(jwt.isValid("not-a-real-token")).isFalse();
            assertThat(jwt.isValid("")).isFalse();
        }
    }

    // ================================================================
    //  CustomUserDetailsService — loading Spring Security principals
    // ================================================================
    @Nested
    @ExtendWith(MockitoExtension.class)
    @DisplayName("CustomUserDetailsService.loadUserByUsername")
    class UserDetailsTests {

        @Mock private UserRepository userRepository;
        @InjectMocks private CustomUserDetailsService userDetailsService;

        @Test
        @DisplayName("maps an active user to enabled UserDetails with a ROLE_ authority")
        void loadsActiveUser() {
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(buyer()));

            UserDetails details = userDetailsService.loadUserByUsername("user@example.com");

            assertThat(details.getUsername()).isEqualTo("user@example.com");
            assertThat(details.getPassword()).isEqualTo("ENCODED_PW");
            assertThat(details.isEnabled()).isTrue();
            assertThat(details.getAuthorities())
                    .extracting(Object::toString)
                    .containsExactly("ROLE_BUYER");
        }

        @Test
        @DisplayName("marks an inactive user as disabled")
        void loadsInactiveUserAsDisabled() {
            User inactive = buyer();
            inactive.setActive(false);
            when(userRepository.findByEmail(eq("user@example.com"))).thenReturn(Optional.of(inactive));

            UserDetails details = userDetailsService.loadUserByUsername("user@example.com");

            assertThat(details.isEnabled()).isFalse();
        }

        @Test
        @DisplayName("throws UsernameNotFoundException for an unknown email")
        void throwsWhenUserMissing() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userDetailsService.loadUserByUsername("ghost@example.com"))
                    .isInstanceOf(UsernameNotFoundException.class);
        }
    }
}
