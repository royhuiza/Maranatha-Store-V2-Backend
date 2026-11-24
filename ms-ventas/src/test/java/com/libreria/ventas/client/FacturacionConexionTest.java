package com.libreria.ventas.client;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class FacturacionConexionTest {

    @Test
    void debeConectarConFacturacionDocker() {

        String token = System.getenv(
                "FACTURACION_INTERNAL_TOKEN");

        assertNotNull(
                token,
                "Credencial interna no configurada");

        assertFalse(token.isBlank());

        FacturacionClient cliente =
                new FacturacionClient(
                        new RestTemplateBuilder(),
                        "http://127.0.0.1:8085"
                );

        ReflectionTestUtils.setField(
                cliente,
                "internalToken",
                token
        );

        assertTrue(
                cliente.verificarConexionInterna(),
                "No se pudo autenticar con MS Facturacion"
        );

        System.out.println(
                "CONEXION MS VENTAS - MS FACTURACION EXITOSA"
        );

        System.out.println(
                "AUTENTICACION INTERNA VALIDADA"
        );
    }
}
