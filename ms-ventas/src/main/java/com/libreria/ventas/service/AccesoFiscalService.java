package com.libreria.ventas.service;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
@Service
public class AccesoFiscalService {
    private final javax.crypto.SecretKey key;
    public AccesoFiscalService(@Value("${jwt.secret}") String secret) {
        key=Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
    public void exigirAdmin(String header) {
        if(header==null || !header.startsWith("Bearer ")) throw noAutorizado();
        io.jsonwebtoken.Claims claims;
        try { claims=Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(header.substring(7).trim()).getBody(); }
        catch(RuntimeException ex) { throw noAutorizado(); }
        if(claims.getSubject()==null || claims.getSubject().isBlank() || claims.getExpiration()==null) throw noAutorizado();
        Object role=claims.get("role");
        if(!"ADMIN".equals(role) && !"ROLE_ADMIN".equals(role)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso denegado");
    }
    private ResponseStatusException noAutorizado() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Token requerido o invalido"); }
}
