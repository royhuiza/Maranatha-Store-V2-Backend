package com.libreria.facturacion.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class InternalAuthenticationFilter
        extends OncePerRequestFilter {

    private final InternalTokenService tokenService;

    public InternalAuthenticationFilter(
            InternalTokenService tokenService
    ) {
        this.tokenService = tokenService;
    }

    @Override
    protected boolean shouldNotFilter(
            HttpServletRequest request
    ) {

        return !"/internal/facturacion/emision/beta"
                .equals(request.getServletPath())
                || !"POST".equals(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String token = request.getHeader(
                "X-Internal-Token"
        );

        if (!tokenService.validar(token)) {

            response.setStatus(
                    HttpServletResponse.SC_UNAUTHORIZED);

            response.setContentType(
                    "application/json");

            response.getWriter().write(
                    "{\"message\":\"Autenticacion interna requerida\"}"
            );

            return;
        }

        var authentication =
                new UsernamePasswordAuthenticationToken(
                        "ms-ventas",
                        null,
                        List.of(
                                new SimpleGrantedAuthority(
                                        "ROLE_SERVICE_VENTAS")
                        )
                );

        var contexto =
                SecurityContextHolder.createEmptyContext();

        contexto.setAuthentication(authentication);

        SecurityContextHolder.setContext(contexto);

        try {

            filterChain.doFilter(request, response);

        } finally {

            SecurityContextHolder.clearContext();
        }
    }
}
