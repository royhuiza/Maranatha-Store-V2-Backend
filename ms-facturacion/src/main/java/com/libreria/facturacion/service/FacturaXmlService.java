package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class FacturaXmlService {

    private static final String INVOICE_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";

    private static final String CBC_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

    private static final String CAC_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";

    public String generarXml(FacturaPruebaRequest factura) {
        try {
            StringWriter salida = new StringWriter();
            XMLStreamWriter xml =
                    XMLOutputFactory.newFactory()
                            .createXMLStreamWriter(salida);

            xml.writeStartDocument("UTF-8", "1.0");

            xml.setDefaultNamespace(INVOICE_NS);
            xml.setPrefix("cbc", CBC_NS);
            xml.setPrefix("cac", CAC_NS);

            xml.writeStartElement("", "Invoice", INVOICE_NS);
            xml.writeDefaultNamespace(INVOICE_NS);
            xml.writeNamespace("cbc", CBC_NS);
            xml.writeNamespace("cac", CAC_NS);

            elemento(xml, "cbc", CBC_NS, "UBLVersionID", "2.1");
            elemento(xml, "cbc", CBC_NS, "CustomizationID", "2.0");
            elemento(xml, "cbc", CBC_NS, "ID",
                    factura.serie() + "-" + factura.numero());
            elemento(xml, "cbc", CBC_NS, "InvoiceTypeCode", "01");
            elemento(xml, "cbc", CBC_NS, "DocumentCurrencyCode", "PEN");

            escribirParte(
                    xml,
                    "AccountingSupplierParty",
                    factura.rucEmisor(),
                    factura.razonSocialEmisor()
            );

            escribirParte(
                    xml,
                    "AccountingCustomerParty",
                    factura.rucCliente(),
                    factura.razonSocialCliente()
            );

            BigDecimal total = BigDecimal.ZERO;
            int numeroLinea = 1;

            for (FacturaPruebaRequest.Detalle detalle : factura.detalles()) {
                BigDecimal subtotal = detalle.precioUnitario()
                        .multiply(BigDecimal.valueOf(detalle.cantidad()))
                        .setScale(2, RoundingMode.HALF_UP);

                total = total.add(subtotal);

                xml.writeStartElement("cac", "InvoiceLine", CAC_NS);

                elemento(xml, "cbc", CBC_NS, "ID",
                        String.valueOf(numeroLinea++));

                elemento(xml, "cbc", CBC_NS, "InvoicedQuantity",
                        detalle.cantidad().toString());

                elemento(xml, "cbc", CBC_NS, "LineExtensionAmount",
                        subtotal.toPlainString());

                xml.writeStartElement("cac", "Item", CAC_NS);
                elemento(xml, "cbc", CBC_NS, "Description",
                        detalle.descripcion());
                xml.writeEndElement();

                xml.writeStartElement("cac", "Price", CAC_NS);
                elemento(xml, "cbc", CBC_NS, "PriceAmount",
                        detalle.precioUnitario().toPlainString());
                xml.writeEndElement();

                xml.writeEndElement();
            }

            xml.writeStartElement("cac", "LegalMonetaryTotal", CAC_NS);
            elemento(xml, "cbc", CBC_NS, "PayableAmount",
                    total.toPlainString());
            xml.writeEndElement();

            xml.writeEndElement();
            xml.writeEndDocument();
            xml.flush();
            xml.close();

            return salida.toString();

        } catch (XMLStreamException ex) {
            throw new IllegalStateException(
                    "No se pudo generar el XML de prueba", ex
            );
        }
    }

    private void escribirParte(
            XMLStreamWriter xml,
            String tipo,
            String ruc,
            String razonSocial
    ) throws XMLStreamException {

        xml.writeStartElement("cac", tipo, CAC_NS);
        xml.writeStartElement("cac", "Party", CAC_NS);

        xml.writeStartElement("cac", "PartyIdentification", CAC_NS);
        elemento(xml, "cbc", CBC_NS, "ID", ruc);
        xml.writeEndElement();

        xml.writeStartElement("cac", "PartyName", CAC_NS);
        elemento(xml, "cbc", CBC_NS, "Name", razonSocial);
        xml.writeEndElement();

        xml.writeEndElement();
        xml.writeEndElement();
    }

    private void elemento(
            XMLStreamWriter xml,
            String prefijo,
            String namespace,
            String nombre,
            String valor
    ) throws XMLStreamException {

        xml.writeStartElement(prefijo, nombre, namespace);
        xml.writeCharacters(valor);
        xml.writeEndElement();
    }
}