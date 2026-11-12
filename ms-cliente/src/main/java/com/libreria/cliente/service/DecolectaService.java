package com.libreria.cliente.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.*;
import java.util.*;

/** Private provider adapter, not a direct government connection. */
@Service
public class DecolectaService {
    private final RestTemplate http;
    private final String apiKey;
    private final Map<String,Cached> cache=new HashMap<>();
    public record Documento(String tipoDocumento,String numeroDocumento,String nombres,
            String apellidoPaterno,String apellidoMaterno,String nombre,String direccion,
            String estado,String condicion,String proveedor) {}
    private record Cached(Documento value,Instant expires) {}

    @Autowired
    public DecolectaService(RestTemplateBuilder builder,
            @Value("${integrations.decolecta.api-key:}") String apiKey) {
        this(builder.setConnectTimeout(Duration.ofSeconds(4)).setReadTimeout(Duration.ofSeconds(8)).build(),apiKey);
    }
    DecolectaService(RestTemplate http,String apiKey) {
        this.http=http; this.apiKey=apiKey==null ? "" : apiKey.trim();
    }

    // Bound short-lived in-memory cache; no persistent identity or secret logging.
    // Serializes low-volume calls to avoid duplicate requests consuming the quota.
    public synchronized Documento consultar(String tipo,String numero) {
        boolean dni="DNI".equals(tipo);
        if ((!dni&&!"RUC".equals(tipo))||numero==null||!numero.matches(dni?"[0-9]{8}":"[0-9]{11}"))
            throw fail(HttpStatus.BAD_REQUEST,"Ingrese un documento con la longitud y los dígitos correctos");
        if(apiKey.isBlank()) throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Falta configurar la clave de Decolecta en el servidor");
        Instant now=Instant.now(); cache.entrySet().removeIf(e->!e.getValue().expires().isAfter(now));
        String key=tipo+":"+numero; Cached previous=cache.get(key); if(previous!=null)return previous.value();
        var headers=new HttpHeaders(); headers.setBearerAuth(apiKey); headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        String url="https://api.decolecta.com/v1/"+(dni?"reniec/dni":"sunat/ruc")+"?numero="+numero;
        try {
            var body=http.exchange(url,HttpMethod.GET,new HttpEntity<Void>(headers),JsonNode.class).getBody();
            Documento result=parse(tipo,numero,body);
            if(cache.size()>=100)cache.clear(); cache.put(key,new Cached(result,now.plusSeconds(300)));return result;
        } catch(HttpStatusCodeException e) {
            int status=e.getStatusCode().value();
            if(status==401||status==403)throw fail(HttpStatus.SERVICE_UNAVAILABLE,"Decolecta rechazó el acceso. Revise la clave y los servicios habilitados en su cuenta");
            if(status==429||status==402)throw fail(HttpStatus.TOO_MANY_REQUESTS,"Decolecta informa un límite de consumo o saldo. Revise su panel antes de reintentar");
            if(status==404)throw fail(HttpStatus.NOT_FOUND,"Decolecta no encontró el documento consultado");
            if(status==400||status==422)throw fail(HttpStatus.BAD_REQUEST,"Decolecta no pudo resolver el documento ingresado");
            throw fail(HttpStatus.BAD_GATEWAY,"Decolecta no está disponible en este momento");
        } catch(ResourceAccessException e) { throw fail(HttpStatus.GATEWAY_TIMEOUT,"No se pudo contactar con Decolecta. Intente más tarde");
        } catch(RestClientException e) { throw fail(HttpStatus.BAD_GATEWAY,"No se pudo interpretar la respuesta de Decolecta"); }
    }

    private Documento parse(String tipo,String numero,JsonNode body) {
        if(body==null||!body.isObject())throw invalid();
        if("DNI".equals(tipo)) {
            String returned=value(body,"document_number"),nombres=value(body,"first_name"),paterno=value(body,"first_last_name"),materno=value(body,"second_last_name");
            if(!numero.equals(returned)||nombres.isBlank()||paterno.isBlank())throw invalid();
            return new Documento(tipo,returned,nombres,paterno,materno,String.join(" ",nombres,paterno,materno).trim(),null,null,null,"DECOLECTA");
        }
        String returned=value(body,"numero_documento"),razon=value(body,"razon_social");
        if(!numero.equals(returned)||razon.isBlank())throw invalid();
        return new Documento(tipo,returned,null,null,null,razon,optional(body,"direccion"),optional(body,"estado"),optional(body,"condicion"),"DECOLECTA");
    }
    private String value(JsonNode body,String name) { var field=body.get(name);return field!=null&&field.isTextual()?field.asText().trim():""; }
    private String optional(JsonNode body,String name) {String v=value(body,name);return v.isBlank()?null:v;}
    private ResponseStatusException invalid(){return fail(HttpStatus.BAD_GATEWAY,"Decolecta devolvió datos incompletos o de otro documento; no se asociará ningún cliente");}
    private ResponseStatusException fail(HttpStatus status,String message){return new ResponseStatusException(status,message);}
}
