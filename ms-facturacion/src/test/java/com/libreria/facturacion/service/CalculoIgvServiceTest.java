package com.libreria.facturacion.service;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import static org.junit.jupiter.api.Assertions.*;

class CalculoIgvServiceTest {

    private final CalculoIgvService service =
            new CalculoIgvService();

    @Test
    void debeCalcularBaseImponible() {
        BigDecimal resultado = service.calcularBaseImponible(
                new BigDecimal("11.80")
        );

        assertEquals(new BigDecimal("10.00"), resultado);
    }

    @Test
    void debeCalcularIgv() {
        BigDecimal resultado = service.calcularIgv(
                new BigDecimal("11.80")
        );

        assertEquals(new BigDecimal("1.80"), resultado);
    }

    @Test
    void debeMantenerPrecioFinal() {
        BigDecimal total = service.calcularTotal(
                new BigDecimal("10.00"),
                new BigDecimal("1.80")
        );

        assertEquals(new BigDecimal("11.80"), total);
    }

    @Test
    void debeRechazarPrecioNegativo() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.calcularIgv(
                        new BigDecimal("-10.00")
                )
        );
    }
}
