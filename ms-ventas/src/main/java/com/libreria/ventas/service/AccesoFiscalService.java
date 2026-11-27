
package com.libreria.ventas.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;

@Service
public class AccesoFiscalService {

    private final SecretKey key;

    public AccesoFiscalService(
            @Value("${jwt.secret}") String secret
    ) {
        key = Keys.hmacShaKeyFor(
                secret.getBytes(StandardCharsets.UTF_8)
        );
    }

    // ACCESO EXCLUSIVO DEL ADMINISTRADOR.

    public void exigirAdmin(String header) {

        Claims claims = validarToken(header);

        Object role = claims.get("role");

        if (!"ADMIN".equals(role)
                && !"ROLE_ADMIN".equals(role)) {

            throw accesoDenegado();
        }
    }

    // ACCESO PARA ADMINISTRADOR Y VENDEDOR.

    public void exigirVendedorOAdmin(String header) {

        Claims claims = validarToken(header);

        Object role = claims.get("role");

        boolean autorizado =
                "ADMIN".equals(role)
                        || "ROLE_ADMIN".equals(role)
                        || "VENDEDOR".equals(role)
                        || "ROLE_VENDEDOR".equals(role);

        if (!autorizado) {

            throw accesoDenegado();
        }
    }

    // VALIDACION DEL TOKEN JWT.

    private Claims validarToken(String header) {

        if (header == null
                || !header.startsWith("Bearer ")) {

            throw noAutorizado();
        }

        Claims claims;

        try {

            claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(
                            header.substring(7).trim()
                    )
                    .getBody();

        } catch (RuntimeException ex) {

            throw noAutorizado();
        }

        if (claims.getSubject() == null
                || claims.getSubject().isBlank()
                || claims.getExpiration() == null) {

            throw noAutorizado();
        }

        return claims;
    }

    private ResponseStatusException noAutorizado() {

        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Token requerido o invalido"
        );
    }

    private ResponseStatusException accesoDenegado() {

        return new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Acceso denegado"
        );
    }
}
