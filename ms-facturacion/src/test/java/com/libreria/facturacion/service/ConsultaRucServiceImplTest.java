
package com.libreria.facturacion.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaRucServiceImplTest {

    @Test
    void debeRechazarRucConMenosDeOnceDigitos() {

        ConsultaRucServiceImpl service =
                new ConsultaRucServiceImpl(false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarPorRuc("12345678")
        );

        assertEquals(
                "El RUC debe contener exactamente 11 digitos",
                error.getMessage()
        );
    }

    @Test
    void debeRechazarRucConLetras() {

        ConsultaRucServiceImpl service =
                new ConsultaRucServiceImpl(false);

        assertThrows(
                IllegalArgumentException.class,
                () -> service.consultarPorRuc("20123456ABC")
        );
    }

    @Test
    void debeRechazarConsultaCuandoEstaDeshabilitada() {

        ConsultaRucServiceImpl service =
                new ConsultaRucServiceImpl(false);

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> service.consultarPorRuc("20123456786")
        );

        assertEquals(
                "La consulta RUC no esta configurada",
                error.getMessage()
        );
    }

    @Test
    void noDebeInventarDatosSiNoExisteProveedor() {

        ConsultaRucServiceImpl service =
                new ConsultaRucServiceImpl(true);

        assertThrows(
                UnsupportedOperationException.class,
                () -> service.consultarPorRuc("20123456786")
        );
    }
}
