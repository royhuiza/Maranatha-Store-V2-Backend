package com.libreria.auth.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class SessionController {
    public record Session(String username, String rol) {}

    @GetMapping("/me")
    public Session me(Authentication authentication) {
        String rol = authentication.getAuthorities().stream().map(org.springframework.security.core.GrantedAuthority::getAuthority)
                .filter(value -> value.startsWith("ROLE_"))
                .findFirst().orElse("ROLE_USER").substring(5);
        return new Session(authentication.getName(), rol);
    }
}
