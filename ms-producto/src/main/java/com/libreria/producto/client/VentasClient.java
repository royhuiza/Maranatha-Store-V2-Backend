package com.libreria.producto.client;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class VentasClient {
    private final RestTemplate http;
    private final String url;
    private final SecretKey key;

    public VentasClient(RestTemplateBuilder builder,
                        @Value("${services.ventas.url}") String url,
                        @Value("${jwt.secret}") String secret) {
        this.http = builder.setConnectTimeout(Duration.ofSeconds(3)).setReadTimeout(Duration.ofSeconds(5)).build();
        this.url = url;
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public boolean tieneVentas(Long productoId) {
        var headers = new HttpHeaders();
        headers.set("X-Internal-Token", Jwts.builder().setSubject("ms-producto")
                .claim("role", "INTERNAL_PRODUCT_HISTORY")
                .setExpiration(Date.from(Instant.now().plusSeconds(30))).signWith(key).compact());
        try {
            Boolean asociado = http.exchange(url + "/ventas/producto/{id}/asociado", HttpMethod.GET,
                    new HttpEntity<>(headers), Boolean.class, productoId).getBody();
            if (asociado != null) return asociado;
        } catch (RestClientException ex) {
            throw noDisponible(ex);
        }
        throw noDisponible(null);
    }

    private ResponseStatusException noDisponible(Throwable cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo verificar el historial de ventas. El producto se conservó.", cause);
    }
}
