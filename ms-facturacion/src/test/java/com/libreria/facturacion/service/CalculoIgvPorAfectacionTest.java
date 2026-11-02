
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.TipoAfectacionIgv;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class CalculoIgvPorAfectacionTest {

    private final CalculoIgvService service =
            new CalculoIgvService();

    @Test
    void debeCalcularProductoGravado() {
        var resultado = service.calcularPorAfectacion(
                new BigDecimal("118.00"),
                TipoAfectacionIgv.GRAVADO
        );

        assertEquals(new BigDecimal("100.00"), resultado.base());
        assertEquals(new BigDecimal("18.00"), resultado.igv());
        assertEquals(new BigDecimal("118.00"), resultado.total());
    }

    @Test
    void debeCalcularProductoExonerado() {
        var resultado = service.calcularPorAfectacion(
                new BigDecimal("50.00"),
                TipoAfectacionIgv.EXONERADO
        );

        assertEquals(new BigDecimal("50.00"), resultado.base());
        assertEquals(new BigDecimal("0.00"), resultado.igv());
        assertEquals(new BigDecimal("50.00"), resultado.total());
    }

    @Test
    void debeCalcularProductoInafecto() {
        var resultado = service.calcularPorAfectacion(
                new BigDecimal("30.00"),
                TipoAfectacionIgv.INAFECTO
        );

        assertEquals(new BigDecimal("30.00"), resultado.base());
        assertEquals(new BigDecimal("0.00"), resultado.igv());
        assertEquals(new BigDecimal("30.00"), resultado.total());
    }

    @Test
    void debeRechazarClasificacionNula() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularPorAfectacion(
                        new BigDecimal("118.00"),
                        null
                )
        );
    }

    @Test
    void debeRechazarPrecioNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularPorAfectacion(
                        new BigDecimal("-10.00"),
                        TipoAfectacionIgv.GRAVADO
                )
        );
    }

    @Test
    void debeCalcularVentaMixta() {
        var gravado = service.calcularPorAfectacion(
                new BigDecimal("118.00"),
                TipoAfectacionIgv.GRAVADO
        );

        var exonerado = service.calcularPorAfectacion(
                new BigDecimal("50.00"),
                TipoAfectacionIgv.EXONERADO
        );

        var inafecto = service.calcularPorAfectacion(
                new BigDecimal("30.00"),
                TipoAfectacionIgv.INAFECTO
        );

        BigDecimal total = gravado.total()
                .add(exonerado.total())
                .add(inafecto.total());

        BigDecimal igvTotal = gravado.igv()
                .add(exonerado.igv())
                .add(inafecto.igv());

        assertEquals(new BigDecimal("198.00"), total);
        assertEquals(new BigDecimal("18.00"), igvTotal);
    }
}
