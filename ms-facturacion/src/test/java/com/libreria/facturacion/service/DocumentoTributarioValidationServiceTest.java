
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.dto.TipoAfectacionIgv;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentoTributarioValidationServiceTest {

    private final DocumentoTributarioValidationService service =
            new DocumentoTributarioValidationService();

    private FacturaPruebaRequest crearFactura(
            String rucEmisor,
            String rucCliente,
            String tipoDocumentoCliente
    ) {
        return new FacturaPruebaRequest(
                rucEmisor,
                "LIBRERIA DEMO",
                rucCliente,
                "CLIENTE DEMO",
                "F001",
                "00000001",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Cuaderno",
                                1,
                                new BigDecimal("11.80"),
                                TipoAfectacionIgv.GRAVADO
                        )
                ),
                "01",
                tipoDocumentoCliente,
                "Lima, Peru",
                LocalDate.of(2026, 11, 2)
        );
    }

    @Test
    void debeAceptarFacturaConFormatoRucCorrecto() {

        var factura = crearFactura(
                "20123456786",
                "20601234565",
                "6"
        );

        assertDoesNotThrow(
                () -> service.validarFactura(factura)
        );
    }

    @Test
    void debeRechazarClienteConDni() {

        var factura = crearFactura(
                "20123456786",
                "12345678",
                "1"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validarFactura(factura)
        );
    }

    @Test
    void debeRechazarRucClienteIncompleto() {

        var factura = crearFactura(
                "20123456786",
                "2060123456",
                "6"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validarFactura(factura)
        );
    }

    @Test
    void debeRechazarRucEmisorConLetras() {

        var factura = crearFactura(
                "20123456ABC",
                "20601234565",
                "6"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validarFactura(factura)
        );
    }

    @Test
    void debeRechazarRucClienteConDigitoVerificadorIncorrecto() {

        var factura = crearFactura(
                "20123456786",
                "20601234567",
                "6"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.validarFactura(factura)
        );

        assertEquals(
                "El RUC del cliente no tiene un formato valido",
                error.getMessage()
        );
    }

    @Test
    void debeRechazarRucEmisorConDigitoVerificadorIncorrecto() {

        var factura = crearFactura(
                "20123456787",
                "20601234565",
                "6"
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> service.validarFactura(factura)
        );

        assertEquals(
                "El RUC del emisor no tiene un formato valido",
                error.getMessage()
        );
    }

}
