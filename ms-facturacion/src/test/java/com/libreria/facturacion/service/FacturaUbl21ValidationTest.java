package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.dto.TipoAfectacionIgv;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaUbl21ValidationTest {

    private final FacturaXmlService facturaXmlService =
            new FacturaXmlService();

    private final XmlValidationService xmlValidationService =
            new XmlValidationService();

    private final FirmaDigitalService firmaDigitalService =
            new FirmaDigitalService();

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

        assertDoesNotThrow(
                () -> firmarYValidar(factura)
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

        assertDoesNotThrow(
                () -> firmarYValidar(factura)
        );
    }

    private void firmarYValidar(
            FacturaPruebaRequest factura
    ) throws Exception {

        Path modulo = Path.of("").toAbsolutePath();

        if (!Files.isDirectory(
                modulo.resolve("src/main/resources/xsd"))) {

            modulo = modulo.resolve("ms-facturacion");
        }

        Path certificado = modulo.resolve(
                "../certificados/maranatha-beta.p12"
        ).normalize();

        Path esquema = modulo.resolve(
                "src/main/resources/xsd/ubl21/maindoc/UBL-Invoice-2.1.xsd"
        );

        assertTrue(
                Files.exists(certificado),
                "No se encontro el certificado digital"
        );

        assertTrue(
                Files.exists(esquema),
                "No se encontro el esquema UBL 2.1"
        );

        String secreto = System.getenv("SUNAT_CERT_PASSWORD");

        assertNotNull(
                secreto,
                "Configure SUNAT_CERT_PASSWORD"
        );

        assertFalse(
                secreto.isBlank(),
                "La contraseña del certificado esta vacia"
        );

        char[] password = secreto.toCharArray();

        try {

            String xml = facturaXmlService.generarXml(factura);

            String xmlFirmado;

            try (InputStream entrada =
                         Files.newInputStream(certificado)) {

                xmlFirmado = firmaDigitalService.firmar(
                        xml,
                        entrada,
                        password,
                        "maranatha-beta"
                );
            }

            assertTrue(
                    xmlFirmado.contains("SignatureValue"),
                    "El XML no contiene la firma digital"
            );

            xmlValidationService.validarXml(
                    xmlFirmado,
                    esquema
            );

            System.out.println(
                    "XML FIRMADO Y VALIDADO UBL 2.1: "
                            + factura.serie()
                            + "-"
                            + factura.numero()
            );

        } finally {
            Arrays.fill(password, '\0');
        }
    }
}