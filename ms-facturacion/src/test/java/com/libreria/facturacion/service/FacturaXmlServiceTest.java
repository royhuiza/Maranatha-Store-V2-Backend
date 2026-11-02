package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.dto.TipoAfectacionIgv;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FacturaXmlServiceTest {

    private static final String CAC_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";

    private static final String CBC_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

    private final FacturaXmlService service =
            new FacturaXmlService();

    @Test
    void debeGenerarXmlConDatosDeFactura() throws Exception {

        FacturaPruebaRequest factura = crearFactura(
                "00000001",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Cuaderno universitario",
                                2,
                                new BigDecimal("11.80"),
                                TipoAfectacionIgv.GRAVADO
                        )
                )
        );

        String xml = service.generarXml(factura);
        Document documento = convertirDocumento(xml);

        assertEquals(
                "Invoice",
                documento.getDocumentElement().getLocalName()
        );

        assertEquals(
                "F001-00000001",
                texto(documento.getDocumentElement(), "ID")
        );

        assertImporte(
                "23.60",
                texto(documento.getDocumentElement(), "PayableAmount")
        );

        assertTrue(xml.contains("Cuaderno universitario"));
    }

    @Test
    void debeCoincidirBaseImponibleConSumaDeLineas() throws Exception {

        FacturaPruebaRequest factura = crearFactura(
                "00000002",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Producto A",
                                1,
                                new BigDecimal("0.01"),
                                TipoAfectacionIgv.GRAVADO
                        ),
                        new FacturaPruebaRequest.Detalle(
                                "Producto B",
                                1,
                                new BigDecimal("0.01"),
                                TipoAfectacionIgv.GRAVADO
                        ),
                        new FacturaPruebaRequest.Detalle(
                                "Producto C",
                                1,
                                new BigDecimal("0.01"),
                                TipoAfectacionIgv.GRAVADO
                        )
                )
        );

        Document documento = convertirDocumento(
                service.generarXml(factura)
        );

        Element totalMonetario = primerElemento(
                documento.getDocumentElement(),
                CAC_NS,
                "LegalMonetaryTotal"
        );

        BigDecimal baseDeclarada = new BigDecimal(
                texto(totalMonetario, "LineExtensionAmount")
        );

        BigDecimal sumaLineas = BigDecimal.ZERO;

        NodeList lineas = documento.getElementsByTagNameNS(
                CAC_NS,
                "InvoiceLine"
        );

        for (int i = 0; i < lineas.getLength(); i++) {

            Element linea = (Element) lineas.item(i);

            sumaLineas = sumaLineas.add(
                    new BigDecimal(
                            texto(linea, "LineExtensionAmount")
                    )
            );
        }

        assertEquals(
                0,
                baseDeclarada.compareTo(sumaLineas),
                "El valor de venta debe coincidir con la suma de las lineas"
        );
    }

    @Test
    void debeGenerarXmlConVentaMixta() throws Exception {

        FacturaPruebaRequest factura = crearFactura(
                "00000003",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Producto gravado",
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

        Document documento = convertirDocumento(
                service.generarXml(factura)
        );

        NodeList lineas = documento.getElementsByTagNameNS(
                CAC_NS,
                "InvoiceLine"
        );

        assertEquals(3, lineas.getLength());

        String[] codigos = {"10", "20", "30"};
        String[] bases = {"100.00", "50.00", "30.00"};
        String[] impuestos = {"18.00", "0.00", "0.00"};
        String[] tributos = {"1000", "9997", "9998"};

        for (int i = 0; i < lineas.getLength(); i++) {

            Element linea = (Element) lineas.item(i);

            Element taxTotal = primerElemento(
                    linea,
                    CAC_NS,
                    "TaxTotal"
            );

            Element taxCategory = primerElemento(
                    linea,
                    CAC_NS,
                    "TaxCategory"
            );

            Element taxScheme = primerElemento(
                    taxCategory,
                    CAC_NS,
                    "TaxScheme"
            );

            assertEquals(
                    codigos[i],
                    textoDirecto(taxCategory, CBC_NS, "ID")
            );

            assertEquals(
                    tributos[i],
                    textoDirecto(taxScheme, CBC_NS, "ID")
            );

            assertImporte(
                    bases[i],
                    textoDirecto(linea, CBC_NS, "LineExtensionAmount")
            );

            assertImporte(
                    impuestos[i],
                    textoDirecto(taxTotal, CBC_NS, "TaxAmount")
            );
        }

        Element totalMonetario = primerElemento(
                documento.getDocumentElement(),
                CAC_NS,
                "LegalMonetaryTotal"
        );

        assertImporte(
                "180.00",
                texto(totalMonetario, "LineExtensionAmount")
        );

        assertImporte(
                "198.00",
                texto(totalMonetario, "PayableAmount")
        );
    }

    @Test
    void debeSepararSubtotalesTributariosGenerales() throws Exception {

        FacturaPruebaRequest factura = crearFactura(
                "00000004",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Gravado",
                                1,
                                new BigDecimal("118.00"),
                                TipoAfectacionIgv.GRAVADO
                        ),
                        new FacturaPruebaRequest.Detalle(
                                "Exonerado",
                                1,
                                new BigDecimal("50.00"),
                                TipoAfectacionIgv.EXONERADO
                        ),
                        new FacturaPruebaRequest.Detalle(
                                "Inafecto",
                                1,
                                new BigDecimal("30.00"),
                                TipoAfectacionIgv.INAFECTO
                        )
                )
        );

        Document documento = convertirDocumento(
                service.generarXml(factura)
        );

        Element taxTotalGeneral = primerElemento(
                documento.getDocumentElement(),
                CAC_NS,
                "TaxTotal"
        );

        assertImporte(
                "18.00",
                textoDirecto(taxTotalGeneral, CBC_NS, "TaxAmount")
        );

        NodeList subtotales = taxTotalGeneral.getElementsByTagNameNS(
                CAC_NS,
                "TaxSubtotal"
        );

        assertEquals(3, subtotales.getLength());

        String[] tributos = {"1000", "9997", "9998"};
        String[] bases = {"100.00", "50.00", "30.00"};
        String[] impuestos = {"18.00", "0.00", "0.00"};

        for (int i = 0; i < subtotales.getLength(); i++) {

            Element subtotal = (Element) subtotales.item(i);

            Element esquema = primerElemento(
                    subtotal,
                    CAC_NS,
                    "TaxScheme"
            );

            assertEquals(
                    tributos[i],
                    textoDirecto(esquema, CBC_NS, "ID")
            );

            assertImporte(
                    bases[i],
                    textoDirecto(subtotal, CBC_NS, "TaxableAmount")
            );

            assertImporte(
                    impuestos[i],
                    textoDirecto(subtotal, CBC_NS, "TaxAmount")
            );
        }
    }

    private FacturaPruebaRequest crearFactura(
            String numero,
            List<FacturaPruebaRequest.Detalle> detalles
    ) {

        return new FacturaPruebaRequest(
                "20123456786",
                "LIBRERIA DEMO",
                "20601234565",
                "CLIENTE DEMO",
                "F001",
                numero,
                detalles
        );
    }

    private Document convertirDocumento(String xml) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-general-entities",
                false
        );

        factory.setFeature(
                "http://xml.org/sax/features/external-parameter-entities",
                false
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder().parse(
                new InputSource(new StringReader(xml))
        );
    }

    private Element primerElemento(
            Element padre,
            String namespace,
            String nombre
    ) {

        NodeList elementos = padre.getElementsByTagNameNS(
                namespace,
                nombre
        );

        assertTrue(
                elementos.getLength() > 0,
                "No se encontro el elemento XML: " + nombre
        );

        return (Element) elementos.item(0);
    }

    private String texto(
            Element padre,
            String nombre
    ) {

        return primerElemento(
                padre,
                CBC_NS,
                nombre
        ).getTextContent();
    }

    private String textoDirecto(
            Element padre,
            String namespace,
            String nombre
    ) {

        NodeList hijos = padre.getChildNodes();

        for (int i = 0; i < hijos.getLength(); i++) {

            if (hijos.item(i) instanceof Element elemento
                    && namespace.equals(elemento.getNamespaceURI())
                    && nombre.equals(elemento.getLocalName())) {

                return elemento.getTextContent();
            }
        }

        fail("No se encontro el elemento directo: " + nombre);
        return "";
    }

    private void assertImporte(
            String esperado,
            String actual
    ) {

        assertEquals(
                0,
                new BigDecimal(esperado).compareTo(
                        new BigDecimal(actual)
                ),
                "Importe incorrecto. Esperado: "
                        + esperado + ", obtenido: " + actual
        );
    }

    @Test
    void debeUtilizarFechaDeEmisionIndicada() throws Exception {

        FacturaPruebaRequest factura = new FacturaPruebaRequest(
                "20123456786",
                "LIBRERIA DEMO",
                "20601234565",
                "CLIENTE DEMO",
                "F001",
                "00000007",
                List.of(
                        new FacturaPruebaRequest.Detalle(
                                "Cuaderno",
                                1,
                                new BigDecimal("11.80"),
                                TipoAfectacionIgv.GRAVADO
                        )
                ),
                "01",
                "6",
                "Lima, Peru",
                java.time.LocalDate.of(2026, 11, 2)
        );

        String xml = service.generarXml(factura);

        Document documento = convertirDocumento(xml);

        assertEquals(
                "2026-11-02",
                texto(documento.getDocumentElement(), "IssueDate")
        );
    }

}
