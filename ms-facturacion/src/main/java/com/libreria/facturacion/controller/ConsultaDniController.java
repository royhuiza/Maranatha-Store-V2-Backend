package com.libreria.facturacion.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Duration;
import java.util.List;

/** Consulta autorizada del documento proporcionado por el comprador. Sin persistir ni registrar datos. */
@RestController
@RequestMapping("/api/facturacion/dni")
public class ConsultaDniController {
    private final RestTemplate http;
    private final String apiKey;
    private final boolean habilitado;
    public ConsultaDniController(RestTemplateBuilder builder,
        @Value("${DECOLECTA_API_KEY:}") String apiKey,
        @Value("${consulta-ruc.enabled:false}") boolean habilitado) {
        this.http = builder.setConnectTimeout(Duration.ofSeconds(5)).setReadTimeout(Duration.ofSeconds(15)).build();
        this.apiKey = apiKey;
        this.habilitado = habilitado;
    }
    @GetMapping("/{dni}")
    public ResponseEntity<Datos> consultar(@PathVariable("dni") String dni) {
        if (!dni.matches("[0-9]{8}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "DNI invalido");
        if (!habilitado || apiKey == null || apiKey.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Consulta de identidad no configurada");
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey.trim());
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        try {
            JsonNode json = http.exchange("https://api.decolecta.com/v1/reniec/dni?numero=" + dni,
                HttpMethod.GET, new HttpEntity<Void>(headers), JsonNode.class).getBody();
            if (json == null || !json.isObject() || !dni.equals(campo(json,"document_number")))
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Respuesta de identidad inconsistente");
            String nombre = campo(json,"first_name"), paterno = campo(json,"first_last_name"), materno = campo(json,"second_last_name");
            if (nombre.isBlank() || (paterno.isBlank() && materno.isBlank())) throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Datos de identidad incompletos");
            return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Datos(dni,nombre,paterno,materno));
        } catch (HttpStatusCodeException e) {
            int c = e.getStatusCode().value();
            if (c == 400 || c == 404 || c == 422) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"DNI no encontrado o invalido");
            if (c == 429) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Limite de consultas alcanzado");
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Proveedor de identidad no disponible o sin autorizacion");
        } catch (ResourceAccessException e) {
            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT,"Consulta de identidad sin respuesta");
        } catch (RestClientException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"Error del proveedor de identidad");
        }
    }
    private static String campo(JsonNode node,String key) { JsonNode x=node.get(key); return x != null && x.isTextual() ? x.asText().trim() : ""; }
    public record Datos(String numeroDocumento,String nombres,String apellidoPaterno,String apellidoMaterno) {}
}
