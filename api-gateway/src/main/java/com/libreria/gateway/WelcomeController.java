package com.libreria.gateway;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class WelcomeController {
    @GetMapping("/")
    public Map<String, Object> welcome() {
        return Map.of(
                "message", "Maranatha Store V2 API Gateway",
                "swagger", "http://localhost:8080/swagger-ui.html",
                "endpoints", Map.of(
                        "login", "POST /auth/login",
                        "productos", "GET/POST /productos",
                        "clientes", "GET/POST /clientes",
                        "ventas", "GET/POST /ventas"
                )
        );
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP", "service", "api-gateway");
    }
}
