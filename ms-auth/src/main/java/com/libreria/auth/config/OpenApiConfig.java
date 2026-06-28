package com.libreria.auth.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .addServersItem(new Server().url("http://localhost:8080").description("API Gateway local"))
                .info(new Info()
                        .title("Maranatha Store - Autenticación")
                        .version("2.0.0")
                        .description("Inicio de sesión y administración de usuarios con JWT"));
    }
}
