package com.bms.backend.auth;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /**
     * GET /api/auth/me : "who am I?". If the request carries a correct username and password,
     * Spring Security has already put the user into the Authentication object. If not, the
     * request never gets here: it is refused with HTTP 401.
     * The React login screen will call this to check a login and to learn the role.
     */
    @GetMapping("/me")
    public CurrentUser me(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .orElse("");
        return new CurrentUser(authentication.getName(), role);
    }
}
