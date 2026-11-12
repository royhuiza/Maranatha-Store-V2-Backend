package com.libreria.facturacion.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * RestTemplate que reenvia el token JWT del usuario a los microservicios
 * internos (ms-cliente).
 *
 * Sin esto, ms-cliente recibe la peticion sin cabecera Authorization y
 * responde 401, que ms-facturacion reenvia al cliente final.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .additionalInterceptors((request, body, execution) -> {
                    var attrs = org.springframework.web.context.request.RequestContextHolder
                            .getRequestAttributes();
                    if (attrs instanceof org.springframework.web.context.request.ServletRequestAttributes servlet) {
                        String token = servlet.getRequest().getHeader("Authorization");
                        if (token != null) {
                            request.getHeaders().set("Authorization", token);
                        }
                    }
                    return execution.execute(request, body);
                })
                .setConnectTimeout(Duration.ofSeconds(4))
                .setReadTimeout(Duration.ofSeconds(10))
                .build();
    }
}
