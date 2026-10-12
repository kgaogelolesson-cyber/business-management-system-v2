package com.bms.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Who may call what. This replaces the temporary "everything open" file from Step 5.
 *
 * Method used: HTTP Basic authentication. Every request carries the username and password in an
 * Authorization header and the server checks them each time. There are no sessions and no cookies.
 * That is simple and good enough for learning, but it is only safe over HTTPS, and a real system
 * would normally hand out a token (JWT) after login. That is a possible later improvement.
 */
@Configuration
public class SecurityConfig {

    /** BCrypt: slow on purpose and salted, so stolen hashes are very hard to crack. */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF attacks abuse browser cookies. We use no cookies (stateless), so it is switched off.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/error").permitAll()
                        // Same rule as Version 1: only the administrator may delete a customer.
                        .requestMatchers(HttpMethod.DELETE, "/api/customers/**").hasRole("ADMIN")
                        // Products: everyone logged in may VIEW them (GET); only the administrator may change them.
                        .requestMatchers(HttpMethod.POST, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/products/**").hasRole("ADMIN")
                        // Inventory: everyone logged in may VIEW stock and history; only the administrator may adjust stock.
                        .requestMatchers(HttpMethod.POST, "/api/inventory/**").hasRole("ADMIN")
                        // Orders: any logged-in user may create and view them; only the administrator may cancel one.
                        .requestMatchers(HttpMethod.POST, "/api/orders/*/cancel").hasRole("ADMIN")
                        // Everything else needs any logged-in user.
                        .anyRequest().authenticated());
        return http.build();
    }
}
