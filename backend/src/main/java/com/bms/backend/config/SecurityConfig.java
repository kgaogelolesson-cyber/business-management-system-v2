package com.bms.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * TEMPORARY. This switches Spring Security's default "everything needs a login" rule OFF
 * so we can test the first endpoint in a browser.
 *
 * It is NOT safe for real use. The login and roles step replaces this file, and from then
 * on every /api URL will require a logged-in user.
 */
@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }
}
