package com.libreria.ventas.controller;
import com.libreria.ventas.service.VentaService;
import jakarta.mail.internet.InternetAddress;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/ventas")
public class EnvioComprobanteController {
 private final org.springframework.jdbc.core.JdbcTemplate jdbc;
 @Value("${comprobantes.public-base-url:http://localhost:8080}") private String publicBase;
 private final VentaService ventas; private final JavaMailSender mail; private final String remitente;
 public EnvioComprobanteController(VentaService ventas, JavaMailSender mail, org.springframework.jdbc.core.JdbcTemplate jdbc, @Value("${spring.mail.username:}") String remitente){this.jdbc=jdbc;this.ventas=ventas;this.mail=mail;this.remitente=remitente;}
 @PostMapping(value="/{id}/enlace", consumes="multipart/form-data")
 public java.util.Map<String,String> enlace(@PathVariable Long id,@RequestParam MultipartFile archivo) throws java.io.IOException {
  ventas.obtenerVenta(id);
  byte[] pdf=archivo.getBytes();
  if(pdf.length<5||pdf.length>8*1024*1024||pdf[0]!=37||pdf[1]!=80||pdf[2]!=68||pdf[3]!=70||pdf[4]!=45) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PDF inválido");
  jdbc.execute("CREATE TABLE IF NOT EXISTS comprobante_enlace (token varchar(64) PRIMARY KEY, venta_id bigint NOT NULL, pdf bytea NOT NULL, vence timestamptz NOT NULL)");
  jdbc.update("DELETE FROM comprobante_enlace WHERE vence < now()");
  byte[] random=new byte[32]; new java.security.SecureRandom().nextBytes(random);
  String token=java.util.HexFormat.of().formatHex(random);
  jdbc.update("INSERT INTO comprobante_enlace(token,venta_id,pdf,vence) VALUES (?,?,?,now()+interval '30 days')",token,id,pdf);
  return java.util.Map.of("url",publicBase.replaceAll("/+$","")+"/ventas/comprobante-publico/"+token);
 }
 @GetMapping("/comprobante-publico/{token}")
 public org.springframework.http.ResponseEntity<byte[]> descargar(@PathVariable String token) {
  if(!token.matches("[a-f0-9]{64}")) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  jdbc.execute("CREATE TABLE IF NOT EXISTS comprobante_enlace (token varchar(64) PRIMARY KEY, venta_id bigint NOT NULL, pdf bytea NOT NULL, vence timestamptz NOT NULL)");
  var filas=jdbc.query("SELECT venta_id,pdf FROM comprobante_enlace WHERE token=? AND vence>now()",(rs,n)->org.springframework.http.ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF).header("Content-Disposition","inline; filename=comprobante-"+rs.getLong(1)+".pdf").header("Cache-Control","no-store").header("X-Content-Type-Options","nosniff").header("Referrer-Policy","no-referrer").body(rs.getBytes(2)),token);
  if(filas.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Enlace inválido o vencido");
  return filas.get(0);
 }
 @PostMapping(value="/{id}/correo", consumes="multipart/form-data")
 @ResponseStatus(HttpStatus.NO_CONTENT)
 public void enviar(@PathVariable Long id,@RequestParam String correo,@RequestParam MultipartFile archivo) {
  var venta=ventas.obtenerVenta(id);
  if(remitente.isBlank()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"SMTP no configurado");
  try {var direccion=new InternetAddress(correo,true);direccion.validate();if(!direccion.getAddress().equals(correo)||!correo.contains("@"))throw new IllegalArgumentException();}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Correo inválido");}
  try {
   byte[] pdf=archivo.getBytes();
   if(pdf.length<5||pdf.length>8*1024*1024||pdf[0]!=37||pdf[1]!=80||pdf[2]!=68||pdf[3]!=70||pdf[4]!=45)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"PDF inválido o demasiado grande");
   var mensaje=mail.createMimeMessage();var helper=new MimeMessageHelper(mensaje,true,"UTF-8");
   helper.setFrom(remitente);helper.setTo(correo);helper.setSubject("Comprobante Maranatha Store");helper.setText("Gracias por su compra. Adjuntamos su comprobante en PDF.");
   helper.addAttachment("comprobante-"+id+".pdf",new ByteArrayResource(pdf),"application/pdf");mail.send(mensaje);
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,"No se pudo enviar el correo mediante SMTP");}
 }
}
