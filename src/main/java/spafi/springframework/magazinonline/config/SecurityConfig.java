package spafi.springframework.magazinonline.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import spafi.springframework.magazinonline.model.Role;

/**
 * Central Spring Security configuration.
 *
 * <p>Authentication is stateless HTTP Basic — appropriate for a REST API — backed by
 * {@link spafi.springframework.magazinonline.security.CustomUserDetailsService} and
 * BCrypt password hashing. Authorization is enforced per endpoint group using the
 * role mapping from the requirements.
 */
@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Stateless REST API: no CSRF tokens, no server-side session.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public: registration and seller account requests.
                        .requestMatchers("/api/auth/register/**").permitAll()
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
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}