package com.libreria.facturacion.security;

import com.libreria.facturacion.controller.ConsultaRucController;
import com.libreria.facturacion.service.ConsultaRucService;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ConsultaRucController.class)
@Import(SecurityConfig.class)
class FacturacionSecurityTest {

    private static final String SECRET_PRUEBA =
            "clave-exclusiva-para-pruebas-jwt-maranatha-store-2026";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultaRucService consultaRucService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private InternalTokenService internalTokenService;

    @Test
    void consultaRucSinTokenDebeRetornar401() throws Exception {

        mockMvc.perform(
                get("/api/facturacion/ruc/20123456786")
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void consultaRucConTokenInvalidoDebeRetornar401() throws Exception {

        mockMvc.perform(
                get("/api/facturacion/ruc/20123456786")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer token-invalido"
                        )
        ).andExpect(status().isUnauthorized());
    }

    @Test
    void usuarioConJwtValidoPuedeConsultarRuc() throws Exception {

        String token = generarToken(
                "usuario.prueba",
                "ROLE_VENDEDOR"
        );

        configurarValidacionJwt();

        mockMvc.perform(
                get("/api/facturacion/ruc/20123456786")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isOk());
    }

    @Test
    void usuarioNormalNoPuedeAccederAOperacionAdministrativa()
            throws Exception {

        String token = generarToken(
                "usuario.prueba",
                "ROLE_VENDEDOR"
        );

        configurarValidacionJwt();

        mockMvc.perform(
                post("/api/facturacion/operacion-administrativa")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isForbidden());
    }

    @Test
    void administradorPuedeAccederARutaDeFacturacion()
            throws Exception {

        String token = generarToken(
                "administrador.prueba",
                "ROLE_ADMIN"
        );

        configurarValidacionJwt();

        mockMvc.perform(
                post("/api/facturacion/operacion-administrativa")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer " + token
                        )
        ).andExpect(status().isNotFound());
    }

    private String generarToken(String usuario, String rol) {

        return Jwts.builder()
                .setSubject(usuario)
                .claim("role", rol)
                .setIssuedAt(new Date())
                .setExpiration(
                        new Date(System.currentTimeMillis() + 60000)
                )
                .signWith(
                        Keys.hmacShaKeyFor(
                                SECRET_PRUEBA.getBytes(
                                        StandardCharsets.UTF_8
                                )
                        )
                )
                .compact();
    }

    private void configurarValidacionJwt() {

        when(jwtService.parse(anyString()))
                .thenAnswer(invocation ->
                        Jwts.parserBuilder()
                                .setSigningKey(
                                        Keys.hmacShaKeyFor(
                                                SECRET_PRUEBA.getBytes(
                                                        StandardCharsets.UTF_8
                                                )
                                        )
                                )
                                .build()
                                .parseClaimsJws(
                                        invocation.getArgument(
                                                0,
                                                String.class
                                        )
                                )
                                .getBody()
                );
    }
}