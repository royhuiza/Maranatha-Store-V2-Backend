
package com.libreria.gateway.filter;

import com.libreria.gateway.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    private JwtService jwtService;
    private JwtAuthFilter jwtAuthFilter;
    private GatewayFilterChain chain;

    @BeforeEach
    void configurar() {
        jwtService = mock(JwtService.class);
        jwtAuthFilter = new JwtAuthFilter(jwtService);
        chain = mock(GatewayFilterChain.class);
    }

    @Test
    void debeRechazarConsultaRucSinToken() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(
                        "/api/facturacion/ruc/20123456786"
                )
        );

        StepVerifier.create(jwtAuthFilter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        verifyNoInteractions(jwtService);
        verifyNoInteractions(chain);
    }

    @Test
    void debeRechazarConsultaRucConTokenInvalido() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(
                        "/api/facturacion/ruc/20123456786"
                ).header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer token-invalido"
                )
        );

        when(jwtService.isValid("token-invalido"))
                .thenReturn(false);

        StepVerifier.create(jwtAuthFilter.filter(exchange, chain))
                .verifyComplete();

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exchange.getResponse().getStatusCode()
        );

        verify(jwtService).isValid("token-invalido");
        verifyNoInteractions(chain);
    }

    @Test
    void debePermitirContinuarConTokenValido() {
        ServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(
                        "/api/facturacion/ruc/20123456786"
                ).header(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer token-valido"
                )
        );

        when(jwtService.isValid("token-valido"))
                .thenReturn(true);

        when(chain.filter(exchange))
                .thenReturn(Mono.empty());

        StepVerifier.create(jwtAuthFilter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService).isValid("token-valido");
        verify(chain).filter(exchange);
    }
}
