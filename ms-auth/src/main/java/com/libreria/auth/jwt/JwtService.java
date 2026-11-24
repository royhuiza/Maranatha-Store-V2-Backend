package com.libreria.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;
    private final com.libreria.auth.repository.UsuarioRepository usuarios;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.expiration-ms}") long expirationMs, com.libreria.auth.repository.UsuarioRepository usuarios) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
        this.usuarios = usuarios;
    }

    public String generateToken(Authentication authentication) {
        String role = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .findFirst()
                .orElse("ROLE_VENDEDOR");
        var usuario = usuarios.findOptionalByUsernameIgnoreCase(authentication.getName()).orElseThrow();
        Date now = new Date();
        return Jwts.builder()
                .setSubject(authentication.getName())
                .claim("role", role)
                .claim("uid", usuario.getId())
                .claim("sv", usuario.getVersionSesion())
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }
}
