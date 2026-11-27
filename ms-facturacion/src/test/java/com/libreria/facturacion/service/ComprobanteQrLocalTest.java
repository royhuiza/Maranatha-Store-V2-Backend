
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.dto.TipoAfectacionIgv;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComprobanteQrLocalTest {

    @Test
    void debeGenerarFirmarYValidarFacturaYBoleta() throws Exception {

        Path modulo = Path.of("").toAbsolutePath();

        if (!Files.isDirectory(
                modulo.resolve("src/main/resources/xsd"))) {
            modulo = modulo.resolve("ms-facturacion");
        }

        Path certificado = modulo.resolve(
                "../certificados/maranatha-beta.p12"
        ).normalize();

        Path xsd = modulo.resolve(
                "src/main/resources/xsd/ubl21/maindoc/UBL-Invoice-2.1.xsd"
        );

        assertTrue(Files.exists(certificado),
                "No se encontro el certificado");

        assertTrue(Files.exists(xsd),
                "No se encontro el esquema XSD");

        String secreto = System.getenv("SUNAT_CERT_PASSWORD");

        assertNotNull(secreto,
                "Configure SUNAT_CERT_PASSWORD");

        char[] password = secreto.toCharArray();

        try {

            for (String tipo : List.of("01", "03")) {

                FacturaPruebaRequest factura =
                        com.libreria.facturacion.controller
                                .ComprobantePrevisualizacionController
                                .ejemplo(tipo);

                FacturaXmlService generador =
                        new FacturaXmlService();

                FirmaDigitalService firmador =
                        new FirmaDigitalService();

                XmlValidationService validador =
                        new XmlValidationService();

                String xml = generador.generarXml(factura);

                String firmado;

                try (InputStream entrada =
                             Files.newInputStream(certificado)) {

                    firmado = firmador.firmar(
                            xml,
                            entrada,
                            password,
                            "maranatha-beta"
                    );
                }

                assertTrue(firmado.contains("SignatureValue"));

                // VERIFICACION CRIPTOGRAFICA.

                KeyStore almacen =
                        KeyStore.getInstance("PKCS12");

                try (InputStream entrada =
                             Files.newInputStream(certificado)) {

                    almacen.load(entrada, password);
                }

                X509Certificate certificadoX509 =
                        (X509Certificate) almacen.getCertificate(
                                "maranatha-beta"
                        );

                assertNotNull(certificadoX509);

                DocumentBuilderFactory factory =
                        DocumentBuilderFactory.newInstance();

                factory.setNamespaceAware(true);

                factory.setFeature(
                        XMLConstants.FEATURE_SECURE_PROCESSING,
                        true);

                factory.setFeature(
                        "http://apache.org/xml/features/disallow-doctype-decl",
                        true);

                factory.setAttribute(
                        XMLConstants.ACCESS_EXTERNAL_DTD, "");

                factory.setAttribute(
                        XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

                Document documento = factory.newDocumentBuilder()
                        .parse(new ByteArrayInputStream(
                                firmado.getBytes(
                                        StandardCharsets.UTF_8)));

                var nodoFirma = documento.getElementsByTagNameNS(
                        "http://www.w3.org/2000/09/xmldsig#",
                        "Signature"
                ).item(0);

                assertNotNull(nodoFirma);

                DOMValidateContext contexto =
                        new DOMValidateContext(
                                certificadoX509.getPublicKey(),
                                nodoFirma
                        );

                contexto.setProperty(
                        "org.jcp.xml.dsig.secureValidation",
                        Boolean.TRUE
                );

                var firma = XMLSignatureFactory
                        .getInstance("DOM")
                        .unmarshalXMLSignature(contexto);

                assertTrue(
                        firma.validate(contexto),
                        "La firma digital no es valida"
                );

                assertDoesNotThrow(
                        () -> firmador.verificarFirma(
                                firmado,
                                certificadoX509
                        )
                );

                System.out.println(
                        "FIRMA DIGITAL VALIDA: " + tipo
                );

                // PRUEBA DE INTEGRIDAD:
                // ALTERAR EL IMPORTE DESPUES DE FIRMAR.

                String xmlAlterado = firmado.replace(
                        ">11.80<",
                        ">99.99<"
                );

                assertNotEquals(
                        firmado,
                        xmlAlterado,
                        "El XML debe haber sido modificado"
                );

                IllegalStateException error =
                        assertThrows(
                                IllegalStateException.class,
                                () -> firmador.verificarFirma(
                                        xmlAlterado,
                                        certificadoX509
                                )
                        );

                assertEquals(
                        "La firma digital del XML no es valida",
                        error.getMessage()
                );

                System.out.println(
                        "OK: XML ALTERADO RECHAZADO " + tipo
                );

                // GUARDAR EL XML ORIGINAL FIRMADO.

                Path salida = modulo.resolve(
                        "target/comprobante-local-" + tipo + ".xml"
                );

                Files.createDirectories(salida.getParent());

                Files.writeString(
                        salida,
                        firmado,
                        StandardCharsets.UTF_8
                );

                // VALIDAR ESTRUCTURA UBL 2.1.

                validador.validarXml(firmado, xsd);

                // VALIDAR CONTENIDO DEL QR.

                var datos = new QrFiscalService()
                        .extraer(firmado);

                assertEquals(
                        tipo,
                        datos.tipoComprobante()
                );

                assertEquals("11.80", datos.total());

                assertEquals("1.80", datos.igv());

                assertEquals(
                        10,
                        datos.contenidoQr()
                                .split("\\|", -1).length
                );

                assertTrue(
                        datos.contenidoQr()
                                .endsWith(datos.hash())
                );

                assertThrows(
                        IllegalArgumentException.class,
                        () -> new QrFiscalService().extraer(xml)
                );

                Files.writeString(
                        modulo.resolve(
                                "target/qr-local-" + tipo + ".txt"
                        ),
                        datos.contenidoQr(),
                        StandardCharsets.UTF_8
                );

                System.out.println(
                        "OK: XML FIRMADO, XSD Y QR LOCAL " + tipo
                );

                System.out.println(
                        "ARCHIVO: " + salida
                );
            }

        } finally {

            Arrays.fill(password, '\0');
        }
    }
}
