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
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EmisionElectronicaTest {

    @Test
    void debeGenerarFirmarYValidarFactura() throws Exception {

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
            FacturaPruebaRequest factura =
                    new FacturaPruebaRequest(
                            "20123456786",
                            "MARANATHA BETA - PRUEBA",
                            "20123456786",
                            "EMPRESA FICTICIA DE PRUEBA",
                            "F001",
                            "00000001",
                            List.of(
                                    new FacturaPruebaRequest.Detalle(
                                            "Producto de prueba",
                                            1,
                                            new BigDecimal("118.00"),
                                            TipoAfectacionIgv.GRAVADO
                                    )
                            ),
                            "01",
                            "6",
                            "DIRECCION DE PRUEBA",
                            LocalDate.now()
                    );

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

            // Verificacion criptografica
            KeyStore almacen = KeyStore.getInstance("PKCS12");

            try (InputStream entrada =
                         Files.newInputStream(certificado)) {
                almacen.load(entrada, password);
            }

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
                            firmado.getBytes(StandardCharsets.UTF_8)));

            var nodoFirma = documento.getElementsByTagNameNS(
                    "http://www.w3.org/2000/09/xmldsig#",
                    "Signature"
            ).item(0);

            assertNotNull(nodoFirma);

            DOMValidateContext contexto =
                    new DOMValidateContext(
                            almacen.getCertificate("maranatha-beta")
                                    .getPublicKey(),
                            nodoFirma
                    );

            var firma = XMLSignatureFactory.getInstance("DOM")
                    .unmarshalXMLSignature(contexto);

            assertTrue(firma.validate(contexto),
                    "La firma digital no es valida");

            System.out.println("FIRMA DIGITAL VALIDA");

            Path salida = modulo.resolve("target/factura-beta.xml");

            Files.createDirectories(salida.getParent());

            Files.writeString(
                    salida, firmado, StandardCharsets.UTF_8);

            validador.validarXml(firmado, xsd);

            System.out.println("XML UBL 2.1 VALIDADO");
            System.out.println("ARCHIVO: " + salida);

        } finally {
            Arrays.fill(password, '\0');
        }
    }
}