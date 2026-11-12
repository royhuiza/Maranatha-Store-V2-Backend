
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.dto.TipoAfectacionIgv;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaUbl21ValidationTest {

    private final FacturaXmlService facturaXmlService =
            new FacturaXmlService();

    private final XmlValidationService xmlValidationService =
            new XmlValidationService();

    @Test
    void debeValidarFacturaConEsquemaUbl21() {

        FacturaPruebaRequest factura = new FacturaPruebaRequest(
                "20123456786",
                "LIBRERIA DEMO",
                "20601234565",
                "CLIENTE DEMO",
                "F001",
                "00000005",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Cuaderno universitario",
                                1,
                                new BigDecimal("118.00"),
                                TipoAfectacionIgv.GRAVADO
                        )
                )
        );

        String xml = facturaXmlService.generarXml(factura);

        Path esquema = Path.of(
                "src",
                "main",
                "resources",
                "xsd",
                "ubl21",
                "maindoc",
                "UBL-Invoice-2.1.xsd"
        );

        assertTrue(
                java.nio.file.Files.exists(esquema),
                "No se encontro el esquema UBL 2.1"
        );

        assertDoesNotThrow(
                () -> xmlValidationService.validarXml(xml, esquema)
        );
    }

    @Test
    void debeValidarFacturaMixtaConEsquemaUbl21() {

        FacturaPruebaRequest factura = new FacturaPruebaRequest(
                "20123456786",
                "LIBRERIA DEMO",
                "20601234565",
                "CLIENTE DEMO",
                "F001",
                "00000006",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Cuaderno",
                                1,
                                new BigDecimal("118.00"),
                                TipoAfectacionIgv.GRAVADO
                        ),
                        new FacturaPruebaRequest.Detalle(
                                "Producto exonerado",
                                1,
                                new BigDecimal("50.00"),
                                TipoAfectacionIgv.EXONERADO
                        ),
                        new FacturaPruebaRequest.Detalle(
                                "Producto inafecto",
                                1,
                                new BigDecimal("30.00"),
                                TipoAfectacionIgv.INAFECTO
                        )
                )
        );

        String xml = facturaXmlService.generarXml(factura);

        Path esquema = Path.of(
                "src",
                "main",
                "resources",
                "xsd",
                "ubl21",
                "maindoc",
                "UBL-Invoice-2.1.xsd"
        );

        assertDoesNotThrow(
                () -> xmlValidationService.validarXml(xml, esquema)
        );
    }

}
