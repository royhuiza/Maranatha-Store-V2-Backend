package com.libreria.ventas.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpStatusCodeException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Component
public class FacturacionClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    @Value("${facturacion.internal.token:}")
    private String internalToken;

    public FacturacionClient(
            RestTemplateBuilder builder,
            @Value("${services.facturacion.url:http://localhost:8085}")
            String baseUrl
    ) {

        this.baseUrl = baseUrl.replaceAll("/+$", "");

        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(90))
                .build();
    }

    public ResultadoEmision emitirBeta(
            SolicitudEmision solicitud
    ) {

        HttpHeaders headers = crearHeaders();

        HttpEntity<SolicitudEmision> entidad =
                new HttpEntity<>(solicitud, headers);

        ResponseEntity<ResultadoEmision> respuesta =
                restTemplate.exchange(
                        baseUrl
                                + "/internal/facturacion/emision/beta",
                        HttpMethod.POST,
                        entidad,
                        ResultadoEmision.class
                );

        ResultadoEmision resultado = respuesta.getBody();

        if (resultado == null) {

            throw new IllegalStateException(
                    "Facturacion devolvio una respuesta vacia");
        }

        return resultado;
    }

    // VERIFICAR CONEXION INTERNA SIN EMITIR FACTURAS

    public boolean verificarConexionInterna() {

        HttpHeaders headers = crearHeaders();

        HttpEntity<String> entidad =
                new HttpEntity<>("{}", headers);

        try {

            restTemplate.exchange(
                    baseUrl
                            + "/internal/facturacion/emision/beta",
                    HttpMethod.POST,
                    entidad,
                    String.class
            );

            return false;

        } catch (HttpStatusCodeException ex) {

            return ex.getStatusCode().value() == 400;
        }
    }

    private HttpHeaders crearHeaders() {

        if (internalToken == null
                || internalToken.isBlank()) {

            throw new IllegalStateException(
                    "Credencial interna no configurada");
        }

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set(
                "X-Internal-Token",
                internalToken
        );

        return headers;
    }

    public record SolicitudEmision(
            String rucEmisor,
            String razonSocialEmisor,
            String rucCliente,
            String razonSocialCliente,
            String serie,
            String numero,
            List<Detalle> detalles,
            String tipoComprobante,
            String tipoDocumentoCliente,
            String direccionEmisor,
            LocalDate fechaEmision
    ) {}

    public record Detalle(
            String descripcion,
            Integer cantidad,
            BigDecimal precioUnitario,
            String afectacionIgv
    ) {}

    public record ResultadoEmision(
            String ambiente,
            String estado,
            String codigo,
            String mensaje,
            String serie,
            String numero
    ) {}
}
