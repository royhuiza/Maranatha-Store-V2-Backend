package com.libreria.ventas.service;
import com.libreria.ventas.model.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
class EstadoComprobanteTest {
    @Test void betaNoEsProduccion() {
        Venta v=new Venta(); v.setTipoComprobante(TipoComprobante.BOLETA); v.setEstadoSunat(EstadoSunat.ACEPTADO_BETA);
        var r=EstadoComprobanteService.construir(v);
        assertEquals("BETA",r.ambiente()); assertFalse(r.validezFiscalConfirmada());
        v.setTipoComprobante(TipoComprobante.NOTA_PEDIDO); assertEquals("NO_APLICA",EstadoComprobanteService.construir(v).estadoSunat());
        v.setTipoComprobante(TipoComprobante.FACTURA); v.setEstadoSunat(null); assertEquals("SIN_REGISTRO",EstadoComprobanteService.construir(v).estadoSunat());
    }
    @Test void accesoAdmin() {
        String secret="test-only-key-not-for-production-123456789";
        var a=new AccesoFiscalService(secret);
        assertEquals(401,assertThrows(ResponseStatusException.class,()->a.exigirAdmin(null)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->a.exigirAdmin(token(secret,"USER"))).getStatusCode().value());
        assertDoesNotThrow(()->a.exigirAdmin(token(secret,"ADMIN")));
        assertEquals(401,assertThrows(ResponseStatusException.class,()->a.exigirAdmin(token(secret+"other","ADMIN"))).getStatusCode().value());
    }
    private String token(String secret,String role) { return "Bearer "+Jwts.builder().setSubject("test").claim("role",role).setExpiration(new Date(System.currentTimeMillis()+60000)).signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact(); }
}
