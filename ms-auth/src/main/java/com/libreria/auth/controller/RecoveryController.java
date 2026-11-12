package com.libreria.auth.controller;
import com.libreria.auth.model.PasswordReset;
import com.libreria.auth.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.util.*;

@RestController @RequestMapping("/auth/password")
public class RecoveryController {
 private final UsuarioRepository users; private final PasswordResetRepository tokens;
 private final ObjectProvider<JavaMailSender> mail; private final PasswordEncoder encoder;
 private final String from,base; private final Map<String,Long> cooldown=new HashMap<>();
 private final SecureRandom random=new SecureRandom();
 public RecoveryController(UsuarioRepository users,PasswordResetRepository tokens,ObjectProvider<JavaMailSender> mail,PasswordEncoder encoder,
   @Value("${recovery.mail-from:}") String from,@Value("${recovery.frontend-url:http://localhost:4200}") String base){this.users=users;this.tokens=tokens;this.mail=mail;this.encoder=encoder;this.from=from;this.base=base;}
 public record Forgot(@NotBlank @Email @Size(max=120) String email){}
 public record Reset(@NotBlank @Size(min=40,max=100) String token,@NotBlank @Size(min=10,max=72) String password){}
 @PostMapping("/forgot") @ResponseStatus(HttpStatus.ACCEPTED) @Transactional
 public void forgot(@Valid @RequestBody Forgot body){
   JavaMailSender sender=mail.getIfAvailable();
   if(sender==null||from.isBlank())throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Recuperación no habilitada: falta configurar el correo de envío");
   String email=body.email().trim().toLowerCase(Locale.ROOT);long now=System.currentTimeMillis();
   synchronized(cooldown){cooldown.entrySet().removeIf(e->now-e.getValue()>60000);if(cooldown.containsKey(email)||cooldown.size()>=1000)throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Espere un minuto antes de volver a solicitarlo");cooldown.put(email,now);}
   var found=users.findOptionalByEmailIgnoreCase(email);if(found.isEmpty())return;
   byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
   var reset=new PasswordReset();reset.tokenHash=hash(token);reset.usuario=found.get();reset.expiresAt=Instant.now().plusSeconds(900);tokens.save(reset);
   var message=new SimpleMailMessage();message.setFrom(from);message.setTo(email);message.setSubject("Recuperación de acceso · Maranatha Store");
   message.setText("Para cambiar tu contraseña, abre este enlace de un solo uso (vence en 15 minutos):\n"+base+"/recuperar-contrasena?token="+token+"\nSi no lo solicitaste, ignora este mensaje.");
   try{sender.send(message);}catch(Exception e){throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo enviar el correo; inténtelo más tarde");}
 }
 @PostMapping("/reset") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional
 public void reset(@Valid @RequestBody Reset body){
   var token=tokens.lockByHash(hash(body.token())).orElseThrow(()->invalid());
   if(token.used||!token.expiresAt.isAfter(Instant.now()))throw invalid();
   token.usuario.setPassword(encoder.encode(body.password()));users.save(token.usuario);token.used=true;tokens.save(token);
 }
 private ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Enlace inválido, vencido o ya utilizado");}
 static String hash(String token){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}catch(NoSuchAlgorithmException e){throw new IllegalStateException(e);}}
}
