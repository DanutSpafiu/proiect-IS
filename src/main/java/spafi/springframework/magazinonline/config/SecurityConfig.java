package spafi.springframework.magazinonline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import spafi.springframework.magazinonline.model.Role;
import spafi.springframework.magazinonline.security.JwtAuthenticationFilter;

/**
 * Central Spring Security configuration.
 *
 * <p>Authentication is stateless and JWT-based: {@code POST /api/auth/login} issues a
 * token (see {@link spafi.springframework.magazinonline.service.AuthService}) and the
 * {@link JwtAuthenticationFilter} validates the {@code Authorization: Bearer} header on
 * every subsequent request. Credentials are checked against
 * {@link spafi.springframework.magazinonline.security.CustomUserDetailsService} with
 * BCrypt hashing. Authorization is enforced per endpoint group using the role mapping
 * from the requirements.
 */
@Configuration
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration)
            throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless REST API: no CSRF tokens, no server-side session.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: login, registration and seller account requests.
                        .requestMatchers("/api/auth/login", "/api/auth/register/**").permitAll()
                        // Public: H2 dev console.
                        .requestMatchers("/h2-console/**").permitAll()
                        // Public + buyer: browse the catalogue.
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()
                        // Admin-only: seller account management.
                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())
                        // Approved sellers: listings and offer decisions.
                        .requestMatchers("/api/seller/**").hasRole(Role.SELLER.name())
                        // Buyers: purchases and offers.
                        .requestMatchers("/api/buyer/**").hasRole(Role.BUYER.name())
                        // Everything else needs authentication (e.g. /api/auth/me).
                        .anyRequest().authenticated())
                // Allow the H2 console to render inside frames.
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // Unauthenticated requests to protected endpoints get 401 (not 403).
                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}