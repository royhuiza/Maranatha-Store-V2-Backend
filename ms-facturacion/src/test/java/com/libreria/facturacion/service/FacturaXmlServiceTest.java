package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.parsers.DocumentBuilderFactory;
import org.xml.sax.InputSource;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaXmlServiceTest {

    private final FacturaXmlService service =
            new FacturaXmlService();

    @Test
    void debeGenerarXmlConDatosDeFactura() throws Exception {

        FacturaPruebaRequest factura = new FacturaPruebaRequest(
                "20123456786",
                "LIBRERIA DEMO",
                "20601234567",
                "CLIENTE DEMO",
                "F001",
                "00000001",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Cuaderno universitario",
                                2,
                                new BigDecimal("11.80")
                        )
                )
        );

        String xml = service.generarXml(factura);

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        Document documento = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        assertEquals(
                "Invoice",
                documento.getDocumentElement().getLocalName()
        );

        assertEquals(
                "F001-00000001",
                documento.getElementsByTagNameNS(
                        "*", "ID"
                ).item(0).getTextContent()
        );

        assertEquals(
                "23.60",
                documento.getElementsByTagNameNS(
                        "*", "PayableAmount"
                ).item(0).getTextContent()
        );

        assertTrue(xml.contains("Cuaderno universitario"));
    }
}
