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

                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/", "/auth", "/dashboard").permitAll()
                        .requestMatchers("/css/**", "/js/**", "/assets/**", "/favicon.ico").permitAll()

                        .requestMatchers("/api/auth/login", "/api/auth/register/**").permitAll()

                        .requestMatchers("/h2-console/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        .requestMatchers("/api/admin/**").hasRole(Role.ADMIN.name())

                        .requestMatchers("/api/seller/**").hasRole(Role.SELLER.name())

                        .requestMatchers("/api/buyer/**").hasRole(Role.BUYER.name())

                        .anyRequest().authenticated())

                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))

                .exceptionHandling(ex ->
                        ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
