package com.libreria.auth.jwt;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

public class JwtAuthenticationToken extends AbstractAuthenticationToken {

    private final String principal;  // Usuario autenticado (username)

    public JwtAuthenticationToken(String principal) {
        super(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));  // Establecemos un rol por defecto
        this.principal = principal;
        setAuthenticated(true);  // Token autenticado
    }

    @Override
    public Object getCredentials() {
        return null;  // No se necesita la contraseña en este caso
    }

    @Override
    public Object getPrincipal() {
        return this.principal;
    }
}