package com.libreria.facturacion.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter
        extends OncePerRequestFilter {

    private static final Logger log =
            LoggerFactory.getLogger(
                    JwtAuthenticationFilter.class);

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String header =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header == null || !header.startsWith("Bearer ")) {

            log.warn("JWT: encabezado Bearer ausente");

        } else if (SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            try {

                String token = header.substring(7).trim();

                Claims claims = jwtService.parse(token);

                String username = claims.getSubject();
                String role = claims.get("role", String.class);

                if (role != null && !role.startsWith("ROLE_")) {
                    role = "ROLE_" + role;
                }

                boolean rolValido =
                        "ROLE_ADMIN".equals(role)
                                || "ROLE_USER".equals(role);

                log.info(
                        "JWT: firma valida, rol reconocido: {}, admin: {}",
                        rolValido,
                        "ROLE_ADMIN".equals(role)
                );

                if (username != null
                        && !username.isBlank()
                        && rolValido) {

                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    List.of(
                                            new SimpleGrantedAuthority(role)
                                    )
                            );

                    SecurityContextHolder.getContext()
                            .setAuthentication(auth);

                    log.info("JWT: autenticacion correcta");

                } else {

                    log.warn(
                            "JWT: usuario o rol no admitido");
                }

            } catch (Exception ex) {

                log.warn(
                        "JWT: validacion fallida - {}",
                        ex.getClass().getSimpleName()
                );

                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }
}
