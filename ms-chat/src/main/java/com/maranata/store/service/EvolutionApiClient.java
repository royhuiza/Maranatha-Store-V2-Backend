package com.maranata.store.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;

/**
 * Adaptador de Evolution API (WhatsApp). Solo consulta y envio de texto:
 * no expone endpoints de cobro ni credenciales de produccion.
 */
@Service
public class EvolutionApiClient {

    private final RestTemplate http;
    private final String baseUrl;
    private final String apiKey;
    private final String instance;

    @org.springframework.beans.factory.annotation.Autowired
    public EvolutionApiClient(RestTemplateBuilder builder,
                              @Value("${services.evolution-api.url:http://maranatha-whatsapp:8080}") String baseUrl,
                              @Value("${integrations.evolution-api.api-key:}") String apiKey,
                              @Value("${integrations.evolution-api.instance:maranatha-store}") String instance) {
        this(builder.setConnectTimeout(Duration.ofSeconds(4)).setReadTimeout(Duration.ofSeconds(10)).build(),
                baseUrl, apiKey, instance);
    }

    EvolutionApiClient(RestTemplate http, String baseUrl, String apiKey, String instance) {
        this.http = http;
        this.baseUrl = baseUrl == null ? "" : baseUrl.trim();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.instance = instance == null || instance.isBlank() ? "maranatha-store" : instance.trim();
    }

    /** Resultado del QR: base64 es el PNG codificado que la app movil muestra. */
    public record Qr(String instancia, String base64, String estado) {}

    public Qr qr() {
        if (apiKey.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "Falta configurar la clave de Evolution API en el servidor");
        }
        try {
            ResponseEntity<String> response = http.exchange(
                    baseUrl + "/instance/connect/" + instance,
                    HttpMethod.GET,
                    new HttpEntity<>(headers()),
                    String.class);
            return parseQr(response.getBody());
        } catch (org.springframework.web.client.HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
                        "La instancia " + instance + " no existe en Evolution API");
            }
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Evolution API no pudo responder el codigo QR");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.GATEWAY_TIMEOUT,
                    "No se pudo contactar con Evolution API. Intente mas tarde");
        }
    }

    private Qr parseQr(String body) {
        if (body == null || body.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Evolution API devolvio una respuesta vacia");
        }
        try {
            JsonNode node = new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
            JsonNode qr = node.has("qrcode") ? node.get("qrcode") : node;
            String base64 = text(qr, "base64");
            if (base64.isBlank()) base64 = text(qr, "code");
            String estado = text(node, "status");
            if (estado.isBlank()) estado = text(node, "state");
            if (base64.isBlank() && estado.isBlank()) {
                throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                        "Evolution API no devolvio un codigo QR utilizable");
            }
            return new Qr(instance, base64, estado);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "No se pudo interpretar la respuesta de Evolution API");
        }
    }

    public void enviarTexto(String numero, String texto) {
        if (numero == null || numero.isBlank() || texto == null || texto.isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Numero y mensaje son obligatorios");
        }
        HttpHeaders headers = headers();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            http.exchange(baseUrl + "/message/sendText/" + instance, HttpMethod.POST,
                    new HttpEntity<>(java.util.Map.of("number", numero, "textMessage", java.util.Map.of("text", texto)), headers),
                    String.class);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY,
                    "Evolution API no pudo enviar el mensaje");
        }
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("apikey", apiKey);
        headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private String text(JsonNode node, String field) {
        if (node == null) return "";
        JsonNode value = node.get(field);
        return value != null && value.isTextual() ? value.asText().trim() : "";
    }
}
