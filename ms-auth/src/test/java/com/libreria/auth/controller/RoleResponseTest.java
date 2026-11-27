package com.libreria.auth.controller;

import com.libreria.auth.dto.LoginRequest;
import com.libreria.auth.jwt.JwtService;
import com.libreria.auth.model.Usuario;
import com.libreria.auth.repository.UsuarioRepository;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.*;
import org.springframework.security.core.GrantedAuthority;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RoleResponseTest {

    @Test
    void roleComesFromAuthorityValueNotLambdaToString() {

        GrantedAuthority authority = () -> "ROLE_ADMIN";

        var authenticated =
            new UsernamePasswordAuthenticationToken(
                "admin-prueba",
                null,
                List.of(authority)
            );

        AuthenticationManager manager =
            request -> authenticated;

        UsuarioRepository usuarios =
            mock(UsuarioRepository.class);

        Usuario usuario = mock(Usuario.class);

        when(usuario.getId()).thenReturn(1L);

        when(usuarios.findOptionalByUsernameIgnoreCase(
            "admin-prueba"
        )).thenReturn(Optional.of(usuario));

        String testSecret =
            UUID.randomUUID().toString() +
            UUID.randomUUID().toString();

        var jwt = new JwtService(
            testSecret,
            60000L,
            usuarios
        );

        var controller =
            new AuthController(jwt, null, manager);

        var request = new LoginRequest();

        request.setUsername("admin-prueba");
        request.setPassword("unit-test-only");

        var response = controller.login(request);

        assertEquals("ADMIN", response.rol());

        assertEquals(
            "ROLE_ADMIN",
            jwt.parse(response.token())
                .get("role", String.class)
        );
    }

    @Test
    void sessionReportsActualAuthorityValue() {

        var authenticated =
            new UsernamePasswordAuthenticationToken(
                "vendedor-prueba",
                null,
                List.of(
                    (GrantedAuthority) () -> "ROLE_VENDEDOR"
                )
            );

        assertEquals(
            "VENDEDOR",
            new SessionController()
                .me(authenticated).rol()
        );
    }
}