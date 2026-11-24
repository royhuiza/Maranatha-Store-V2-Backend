package com.libreria.facturacion.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InternalTokenServiceTest {

    private final InternalTokenService servicio =
            new InternalTokenService(
                    "credencial-ficticia-para-test"
            );

    @Test
    void debeAceptarTokenCorrecto() {

        assertTrue(servicio.validar(
                "credencial-ficticia-para-test"
        ));
    }

    @Test
    void debeRechazarTokenIncorrecto() {

        assertFalse(servicio.validar(
                "token-incorrecto"
        ));
    }

    @Test
    void debeRechazarTokenVacio() {

        assertFalse(servicio.validar(""));
    }

    @Test
    void debeRechazarTokenNulo() {

        assertFalse(servicio.validar(null));
    }

    @Test
    void debeRechazarConfiguracionVacia() {

        InternalTokenService sinConfiguracion =
                new InternalTokenService("");

        assertFalse(
                sinConfiguracion.validar("cualquier-token")
        );
    }
}
