package com.libreria.producto.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TipoAfectacionIgvTest {

    @Test
    void debeReconocerCodigosSunat() {
        assertEquals("10", TipoAfectacionIgv.GRAVADO.getCodigoSunat());
        assertEquals("20", TipoAfectacionIgv.EXONERADO.getCodigoSunat());
        assertEquals("30", TipoAfectacionIgv.INAFECTO.getCodigoSunat());
    }

    @Test
    void debeReconocerClasificaciones() {
        assertEquals(
                TipoAfectacionIgv.GRAVADO,
                TipoAfectacionIgv.valueOf("GRAVADO")
        );

        assertEquals(
                TipoAfectacionIgv.EXONERADO,
                TipoAfectacionIgv.valueOf("EXONERADO")
        );

        assertEquals(
                TipoAfectacionIgv.INAFECTO,
                TipoAfectacionIgv.valueOf("INAFECTO")
        );
    }
}
