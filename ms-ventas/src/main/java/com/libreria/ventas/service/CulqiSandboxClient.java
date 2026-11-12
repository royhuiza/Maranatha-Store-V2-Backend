package com.libreria.ventas.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.*;

/** Prepared adapter only. No HTTP charge endpoint or production mode is enabled. */
@Service
public class CulqiSandboxClient {
    private final RestTemplate http;
    private final String testKey;

    @Autowired
    public CulqiSandboxClient(RestTemplateBuilder builder,
            @Value("${integrations.culqi.test-secret-key:}") String testKey) {
        this(builder.setConnectTimeout(Duration.ofSeconds(4)).setReadTimeout(Duration.ofSeconds(10)).build(), testKey);
    }
    CulqiSandboxClient(RestTemplate http, String testKey) { this.http=http;this.testKey=testKey==null?"":testKey.trim(); }
    public record CargoPrueba(String id, long centimos, String moneda, String estado, boolean sandbox) {}

    // Amount must come from the stored sale, never from an Angular price field.
    public CargoPrueba crearCargoPrueba(BigDecimal totalServidor, String tokenPrueba, String email) {
        if (!testKey.startsWith("sk_test_")) throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Falta una llave privada de pruebas de Culqi. Producción no está habilitada");
        if (tokenPrueba==null || !tokenPrueba.startsWith("tkn_test_")) throw fail(HttpStatus.BAD_REQUEST,"Este adaptador solo acepta tokens de tarjeta del entorno de pruebas oficial");
        long amount;
        try { amount=totalServidor.movePointRight(2).longValueExact(); }
        catch (RuntimeException e) { throw fail(HttpStatus.BAD_REQUEST,"Monto de prueba inválido"); }
        if (amount<=0 || email==null || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) throw fail(HttpStatus.BAD_REQUEST,"Monto o correo de prueba inválido");
        var headers=new HttpHeaders(); headers.setBearerAuth(testKey); headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            JsonNode data=http.postForObject("https://api.culqi.com/v2/charges",new HttpEntity<>(Map.of("amount",amount,"currency_code","PEN","email",email,"source_id",tokenPrueba),headers),JsonNode.class);
            String id=data==null?"":data.path("id").asText();
            if(data==null || !"charge".equals(data.path("object").asText()) || !id.startsWith("chr_test_")
                    || data.path("amount").asLong(-1)!=amount || !"PEN".equals(data.path("currency").asText())
                    || !"venta_exitosa".equals(data.path("response_code").asText())) {
                throw fail(HttpStatus.BAD_GATEWAY,"No se pudo verificar el cargo de prueba; no se confirmará una venta real");
            }
            return new CargoPrueba(id,amount,"PEN","APROBADO_EN_SANDBOX",true);
        } catch(HttpStatusCodeException e) {
            if(e.getStatusCode().value()==401||e.getStatusCode().value()==403) throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Culqi rechazó la configuración de pruebas");
            throw fail(HttpStatus.BAD_GATEWAY,"Culqi rechazó o no pudo procesar la solicitud de prueba");
        } catch(ResourceAccessException e) { throw fail(HttpStatus.GATEWAY_TIMEOUT,"Resultado de prueba desconocido. Verifique el panel antes de reintentar");
        } catch(RestClientException e) { throw fail(HttpStatus.BAD_GATEWAY,"No se pudo interpretar la respuesta de Culqi"); }
    }
    private ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
