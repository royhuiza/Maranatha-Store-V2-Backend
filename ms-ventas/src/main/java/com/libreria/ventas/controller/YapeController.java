package com.libreria.ventas.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import jakarta.annotation.PostConstruct;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/ventas/yape")
public class YapeController {
 private final JdbcTemplate db;
 public YapeController(JdbcTemplate db){this.db=db;}
 @PostConstruct void init(){
  db.execute("CREATE TABLE IF NOT EXISTS yape_dispositivo (id integer PRIMARY KEY, token_hash varchar(64) NOT NULL)");
  db.execute("CREATE TABLE IF NOT EXISTS yape_evento (id varchar(64) PRIMARY KEY, monto numeric(12,2) NOT NULL, fecha bigint NOT NULL, recibido timestamptz NOT NULL DEFAULT now())");
 }
 private String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
 @PostMapping("/dispositivo") public Map<String,String> activar(){
  byte[] bytes=new byte[32];new SecureRandom().nextBytes(bytes);String token=HexFormat.of().formatHex(bytes);
  db.update("INSERT INTO yape_dispositivo(id,token_hash) VALUES(1,?) ON CONFLICT(id) DO UPDATE SET token_hash=excluded.token_hash",hash(token));return Map.of("token",token);
 }
 @DeleteMapping("/dispositivo") public void desactivar(){db.update("DELETE FROM yape_dispositivo WHERE id=1");}
 public record Evento(String id, BigDecimal monto, Long fecha){}
 @PostMapping("/eventos") public Map<String,String> recibir(@RequestHeader(value="X-Yape-Token",required=false) String token,@RequestBody Evento e){
  if(token==null || !token.matches("[a-f0-9]{64}") || db.queryForObject("SELECT count(*) FROM yape_dispositivo WHERE id=1 AND token_hash=?",Integer.class,hash(token))!=1) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
  if(e.id()==null || !e.id().matches("[a-f0-9]{64}") || e.monto()==null || e.monto().signum()<=0 || e.monto().scale()>2 || e.monto().compareTo(new BigDecimal("9999999999.99"))>0 || e.fecha()==null || e.fecha()<=0 || e.fecha()>System.currentTimeMillis()+300000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
  db.update("INSERT INTO yape_evento(id,monto,fecha) VALUES(?,?,?) ON CONFLICT(id) DO NOTHING",e.id(),e.monto(),e.fecha());return Map.of("estado","DETECTADO");
 }
 @GetMapping("/eventos") public List<Map<String,Object>> listar(){return db.queryForList("SELECT e.id,e.monto,e.fecha FROM yape_evento e WHERE NOT EXISTS (SELECT 1 FROM venta_operaciones o WHERE o.idempotency_key=concat('YAPE-',e.id)) ORDER BY e.fecha DESC LIMIT 30");}
}
