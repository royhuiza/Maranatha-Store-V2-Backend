
package com.libreria.facturacion.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.libreria.facturacion.dto.ConsultaRucResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

@Service
public class ConsultaRucServiceImpl implements ConsultaRucService {

    private final boolean consultaHabilitada;
    private final RestTemplate http;
    private final String clienteServiceUrl;

    @Autowired
    public ConsultaRucServiceImpl(
            @Value("${consulta-ruc.enabled:false}")
            boolean consultaHabilitada,
            @Value("${consulta-ruc.cliente-url:http://ms-cliente:8083}")
            String clienteServiceUrl,
            RestTemplate restTemplate
    ) {
        this.consultaHabilitada = consultaHabilitada;
        this.clienteServiceUrl = clienteServiceUrl;
        // El token del usuario lo agrega el interceptor del RestTemplate
        // (ver RestTemplateConfig): ms-cliente lo exige para responder.
        this.http = restTemplate;
    }

    // Compatibilidad con las pruebas existentes.
    public ConsultaRucServiceImpl(boolean consultaHabilitada) {
        this.consultaHabilitada = consultaHabilitada;
        this.clienteServiceUrl = "";
        this.http = new RestTemplate();
    }

    @Override
    public ConsultaRucResponse consultarPorRuc(String ruc) {

        if (ruc == null || !ruc.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "El RUC debe contener exactamente 11 digitos"
            );
        }

        if (!consultaHabilitada) {
            throw new IllegalStateException(
                    "La consulta RUC no esta configurada"
            );
        }

        if (clienteServiceUrl.isBlank()) {
            throw new IllegalStateException(
                    "No se ha configurado ms-cliente"
            );
        }

        String url = clienteServiceUrl
                + "/clientes/documentos/ruc/" + ruc;

        try {
            ResponseEntity<JsonNode> respuesta =
                    http.exchange(
                            url,
                            HttpMethod.GET,
                            new HttpEntity<Void>(new HttpHeaders()),
                            JsonNode.class
                    );

            JsonNode datos = respuesta.getBody();

            if (datos == null || !datos.isObject()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Respuesta RUC invalida"
                );
            }

            String numero = texto(datos, "numeroDocumento");
            String razonSocial = texto(datos, "nombre");

            if (!ruc.equals(numero) || razonSocial.isBlank()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "El proveedor no devolvio datos validos para el RUC"
                );
            }

            return new ConsultaRucResponse(
                    numero,
                    razonSocial,
                    texto(datos, "estado"),
                    texto(datos, "condicion"),
                    texto(datos, "direccion")
            );

        } catch (HttpStatusCodeException error) {
            HttpStatusCode status = error.getStatusCode();

            if (status.value() == 404) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontraron datos para el RUC"
                );
            }

            if (status.value() == 429) {
                throw new ResponseStatusException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        "Limite de consultas alcanzado"
                );
            }

            if (status.value() == 400) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El proveedor rechazo el RUC"
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo consultar el servicio de RUC"
            );

        } catch (ResourceAccessException error) {
            throw new ResponseStatusException(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "No se pudo conectar con ms-cliente"
            );

        } catch (RestClientException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Error al interpretar la respuesta de ms-cliente"
            );
        }
    }

    private String texto(JsonNode datos, String campo) {
        JsonNode valor = datos.get(campo);

        return valor != null && valor.isTextual()
                ? valor.asText().trim()
                : "";
    }
}
