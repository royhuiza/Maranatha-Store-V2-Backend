package com.libreria.auth.controller;

import com.libreria.auth.config.SecurityConfig;
import com.libreria.auth.jwt.*;
import com.libreria.auth.model.*;
import com.libreria.auth.repository.UsuarioRepository;
import com.libreria.auth.service.*;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Date;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class UserPermissionsTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    JwtService jwt;

    @MockBean
    UsuarioRepository users;

    @MockBean
    AuthService service;

    @MockBean
    UsuarioDetailsService details;

    private void login(String name, Rol role) {

        Usuario user = mock(Usuario.class);

        when(user.getId()).thenReturn(1L);
        when(user.getUsername()).thenReturn(name);
        when(user.getRol()).thenReturn(role);
        when(user.isActivo()).thenReturn(true);

        Claims claims = Jwts.claims()
            .setSubject(name)
            .setExpiration(
                new Date(System.currentTimeMillis() + 60000)
            );

        claims.put("uid", user.getId());
        claims.put("sv", Long.valueOf(user.getVersionSesion()));
        claims.put("role", "ROLE_" + role.name());

        when(jwt.parse(name)).thenReturn(claims);

        when(users.findOptionalByUsernameIgnoreCase(name))
            .thenReturn(Optional.of(user));
    }

    @Test
    void sellerCannotListUsers() throws Exception {

        login("seller", Rol.VENDEDOR);

        mvc.perform(
            get("/auth/usuarios")
                .header("Authorization", "Bearer seller")
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void sellerCannotCreateAdmin() throws Exception {

        login("seller", Rol.VENDEDOR);

        mvc.perform(
            post("/auth/usuarios")
                .header("Authorization", "Bearer seller")
                .contentType("application/json")
                .content(
                    "{\"username\":\"new-admin\","
                    + "\"password\":\"long-password\","
                    + "\"email\":\"test@example.invalid\","
                    + "\"rol\":\"ADMIN\"}"
                )
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }

    @Test
    void adminCanListUsers() throws Exception {

        login("admin", Rol.ADMIN);

        mvc.perform(
            get("/auth/usuarios")
                .header("Authorization", "Bearer admin")
        ).andExpect(status().isOk());

        verify(service).listarUsuarios();
    }

    @Test
    void anonymousCannotReadUsers() throws Exception {

        mvc.perform(
            get("/auth/usuarios")
        ).andExpect(status().isForbidden());

        verifyNoInteractions(service);
    }
}