package com.libreria.auth.controller;
import com.libreria.auth.model.*;
import com.libreria.auth.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
class RecoveryControllerTest {
 @Test void consumesValidTokenAndHashesPassword(){var users=mock(UsuarioRepository.class);var repo=mock(PasswordResetRepository.class);var encoder=mock(PasswordEncoder.class);var t=new PasswordReset();t.usuario=new Usuario();t.expiresAt=Instant.now().plusSeconds(60);when(repo.lockByHash(anyString())).thenReturn(Optional.of(t));when(encoder.encode("new-password")).thenReturn("hash");var c=controller(users,repo,encoder);c.reset(new RecoveryController.Reset("valid-token","new-password"));assertTrue(t.used);assertEquals("hash",t.usuario.getPassword());verify(users).save(t.usuario);}
 @Test void rejectsExpiredToken(){var users=mock(UsuarioRepository.class);var repo=mock(PasswordResetRepository.class);var t=new PasswordReset();t.expiresAt=Instant.now().minusSeconds(1);when(repo.lockByHash(anyString())).thenReturn(Optional.of(t));assertThrows(ResponseStatusException.class,()->controller(users,repo,mock(PasswordEncoder.class)).reset(new RecoveryController.Reset("token","new-password")));verifyNoInteractions(users);}
 @Test void rejectsReusedToken(){var users=mock(UsuarioRepository.class);var repo=mock(PasswordResetRepository.class);var t=new PasswordReset();t.used=true;t.expiresAt=Instant.now().plusSeconds(60);when(repo.lockByHash(anyString())).thenReturn(Optional.of(t));assertThrows(ResponseStatusException.class,()->controller(users,repo,mock(PasswordEncoder.class)).reset(new RecoveryController.Reset("token","new-password")));verifyNoInteractions(users);}
 @SuppressWarnings("unchecked") private RecoveryController controller(UsuarioRepository u,PasswordResetRepository r,PasswordEncoder p){return new RecoveryController(u,r,new org.springframework.beans.factory.support.DefaultListableBeanFactory().getBeanProvider(JavaMailSender.class),p,"","http://localhost:4200");}
}
