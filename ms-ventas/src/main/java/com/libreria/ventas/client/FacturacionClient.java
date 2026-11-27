
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

    // EMISION DE COMPROBANTES SUNAT BETA

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

    // FIRMA DIGITAL LOCAL DE COMPROBANTES COMERCIALES

    public ResultadoFirma firmarLocal(
            SolicitudEmision solicitud
    ) {

        HttpHeaders headers = crearHeaders();

        HttpEntity<SolicitudEmision> entidad =
                new HttpEntity<>(solicitud, headers);

        ResponseEntity<ResultadoFirma> respuesta =
                restTemplate.exchange(
                        baseUrl
                                + "/internal/facturacion/emision/local",
                        HttpMethod.POST,
                        entidad,
                        ResultadoFirma.class
                );

        ResultadoFirma resultado = respuesta.getBody();

        if (resultado == null) {

            throw new IllegalStateException(
                    "El servicio de facturacion no devolvio la firma local");
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

    // CONFIGURACION DE AUTENTICACION INTERNA

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

    // SOLICITUD DE EMISION DEL COMPROBANTE

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

    // DETALLE DE PRODUCTOS DEL COMPROBANTE

    public record Detalle(
            String descripcion,
            Integer cantidad,
            BigDecimal precioUnitario,
            String afectacionIgv
    ) {}

    // RESPUESTA DE EMISION SUNAT BETA

    public record ResultadoEmision(
            String ambiente,
            String estado,
            String codigo,
            String mensaje,
            String serie,
            String numero
    ) {}

    // RESPUESTA DE FIRMA DIGITAL LOCAL

    public record ResultadoFirma(
            String ambiente,
            String estado,
            boolean validezFiscal,
            String serie,
            String numero,
            String igv,
            String total,
            String hash,
            String contenidoQr,
            String xmlFirmado
    ) {}
}
