package com.libreria.ventas.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder, @org.springframework.beans.factory.annotation.Value("${jwt.secret}") String secret) {
        return builder
                .additionalInterceptors((request, body, execution) -> {
                    var attrs = org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
                    if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
                        String token = servlet.getRequest().getHeader("Authorization");
                        if (token != null) request.getHeaders().set("Authorization", token);
                    }
                    if (request.getURI().getPath().matches("/productos/(\\d+/stock/(descontar|reponer)|stock/venta/[^/]+/(descontar|revertir))")) {
                        var key=io.jsonwebtoken.security.Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                        var token=io.jsonwebtoken.Jwts.builder().setSubject("ms-ventas").claim("role","INTERNAL_STOCK")
                            .setExpiration(new java.util.Date(System.currentTimeMillis()+30000)).signWith(key).compact();
                        request.getHeaders().set("X-Internal-Token",token);
                    }
                    return execution.execute(request, body);
                })
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(8))
                .build();
    }
}
