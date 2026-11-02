package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.dto.TipoAfectacionIgv;
import org.springframework.stereotype.Service;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
public class FacturaXmlService {

    private static final String INVOICE_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:Invoice-2";

    private static final String CBC_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";

    private static final String CAC_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";

    private static final BigDecimal FACTOR_IGV =
            new BigDecimal("1.18");

    private final CalculoIgvService calculoIgvService =
            new CalculoIgvService();

    private final DocumentoTributarioValidationService
            documentoTributarioValidationService =
            new DocumentoTributarioValidationService();

    public String generarXml(FacturaPruebaRequest factura) {

        documentoTributarioValidationService.validarFactura(factura);

        try {
            StringWriter salida = new StringWriter();

            XMLStreamWriter xml = XMLOutputFactory.newFactory()
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

            elemento(
                    xml, "cbc", CBC_NS, "ID",
                    factura.serie() + "-" + factura.numero()
            );

            LocalDate fechaEmision = factura.fechaEmision();

            if (fechaEmision == null) {
                throw new IllegalArgumentException(
                        "La fecha de emision es obligatoria"
                );
            }

            elemento(
                    xml, "cbc", CBC_NS, "IssueDate",
                    fechaEmision.toString()
            );

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

            // 1. CALCULAR LOS TOTALES DE LA VENTA

            BigDecimal totalConIgv = BigDecimal.ZERO;
            BigDecimal baseImponible = BigDecimal.ZERO;
            BigDecimal igv = BigDecimal.ZERO;

            BigDecimal totalGravado = BigDecimal.ZERO;
            BigDecimal totalExonerado = BigDecimal.ZERO;
            BigDecimal totalInafecto = BigDecimal.ZERO;

            for (FacturaPruebaRequest.Detalle detalle : factura.detalles()) {

                BigDecimal subtotal = detalle.precioUnitario()
                        .multiply(BigDecimal.valueOf(detalle.cantidad()))
                        .setScale(2, RoundingMode.HALF_UP);

                var resultado = calculoIgvService.calcularPorAfectacion(
                        subtotal,
                        detalle.afectacionIgv()
                );

                totalConIgv = totalConIgv.add(resultado.total());
                baseImponible = baseImponible.add(resultado.base());
                igv = igv.add(resultado.igv());

                switch (detalle.afectacionIgv()) {
                    case GRAVADO ->
                            totalGravado = totalGravado.add(resultado.base());

                    case EXONERADO ->
                            totalExonerado = totalExonerado.add(resultado.base());

                    case INAFECTO ->
                            totalInafecto = totalInafecto.add(resultado.base());
                }
            }

            // 2. IMPUESTOS GENERALES DEL COMPROBANTE

            xml.writeStartElement("cac", "TaxTotal", CAC_NS);

            importe(xml, "TaxAmount", igv);

            if (totalGravado.signum() > 0) {
                escribirSubtotalTributario(
                        xml,
                        totalGravado,
                        igv,
                        "1000",
                        "IGV",
                        "VAT"
                );
            }

            if (totalExonerado.signum() > 0) {
                escribirSubtotalTributario(
                        xml,
                        totalExonerado,
                        BigDecimal.ZERO.setScale(2),
                        "9997",
                        "EXO",
                        "VAT"
                );
            }

            if (totalInafecto.signum() > 0) {
                escribirSubtotalTributario(
                        xml,
                        totalInafecto,
                        BigDecimal.ZERO.setScale(2),
                        "9998",
                        "INA",
                        "FRE"
                );
            }

            xml.writeEndElement(); // TaxTotal

            // 3. TOTALES MONETARIOS

            xml.writeStartElement(
                    "cac", "LegalMonetaryTotal", CAC_NS
            );

            importe(xml, "LineExtensionAmount", baseImponible);
            importe(xml, "TaxInclusiveAmount", totalConIgv);
            importe(xml, "PayableAmount", totalConIgv);

            xml.writeEndElement(); // LegalMonetaryTotal

            // 4. DETALLES DE LA FACTURA

            int numeroLinea = 1;

            for (FacturaPruebaRequest.Detalle detalle : factura.detalles()) {

                BigDecimal subtotalConIgv = detalle.precioUnitario()
                        .multiply(BigDecimal.valueOf(detalle.cantidad()))
                        .setScale(2, RoundingMode.HALF_UP);

                var resultadoLinea = calculoIgvService.calcularPorAfectacion(
                        subtotalConIgv,
                        detalle.afectacionIgv()
                );

                BigDecimal subtotalSinIgv = resultadoLinea.base();

                BigDecimal precioSinIgv;

                if (detalle.afectacionIgv() == TipoAfectacionIgv.GRAVADO) {
                    precioSinIgv = detalle.precioUnitario()
                            .divide(
                                    FACTOR_IGV,
                                    10,
                                    RoundingMode.HALF_UP
                            );
                } else {
                    precioSinIgv = detalle.precioUnitario();
                }

                xml.writeStartElement(
                        "cac", "InvoiceLine", CAC_NS
                );

                elemento(
                        xml, "cbc", CBC_NS, "ID",
                        String.valueOf(numeroLinea++)
                );

                xml.writeStartElement(
                        "cbc", "InvoicedQuantity", CBC_NS
                );

                xml.writeAttribute("unitCode", "NIU");
                xml.writeCharacters(detalle.cantidad().toString());
                xml.writeEndElement();

                importe(
                        xml, "LineExtensionAmount", subtotalSinIgv
                );

                // 5. IMPUESTO INDIVIDUAL DE LA LINEA

                xml.writeStartElement(
                        "cac", "TaxTotal", CAC_NS
                );

                importe(
                        xml, "TaxAmount", resultadoLinea.igv()
                );

                xml.writeStartElement(
                        "cac", "TaxSubtotal", CAC_NS
                );

                importe(
                        xml, "TaxableAmount", resultadoLinea.base()
                );

                importe(
                        xml, "TaxAmount", resultadoLinea.igv()
                );

                xml.writeStartElement(
                        "cac", "TaxCategory", CAC_NS
                );

                elemento(
                        xml, "cbc", CBC_NS, "ID",
                        detalle.afectacionIgv().getCodigoSunat()
                );

                String codigoTributo;
                String nombreTributo;
                String tipoTributo;

                switch (detalle.afectacionIgv()) {

                    case GRAVADO -> {
                        codigoTributo = "1000";
                        nombreTributo = "IGV";
                        tipoTributo = "VAT";
                    }

                    case EXONERADO -> {
                        codigoTributo = "9997";
                        nombreTributo = "EXO";
                        tipoTributo = "VAT";
                    }

                    case INAFECTO -> {
                        codigoTributo = "9998";
                        nombreTributo = "INA";
                        tipoTributo = "FRE";
                    }

                    default -> throw new IllegalArgumentException(
                            "Clasificación tributaria no soportada"
                    );
                }

                xml.writeStartElement(
                        "cac", "TaxScheme", CAC_NS
                );

                elemento(
                        xml, "cbc", CBC_NS, "ID", codigoTributo
                );

                elemento(
                        xml, "cbc", CBC_NS, "Name", nombreTributo
                );

                elemento(
                        xml, "cbc", CBC_NS, "TaxTypeCode", tipoTributo
                );

                xml.writeEndElement(); // TaxScheme
                xml.writeEndElement(); // TaxCategory
                xml.writeEndElement(); // TaxSubtotal
                xml.writeEndElement(); // TaxTotal

                // 6. DESCRIPCION DEL PRODUCTO

                xml.writeStartElement(
                        "cac", "Item", CAC_NS
                );

                elemento(
                        xml, "cbc", CBC_NS, "Description",
                        detalle.descripcion()
                );

                xml.writeEndElement(); // Item

                // 7. PRECIO UNITARIO

                xml.writeStartElement(
                        "cac", "Price", CAC_NS
                );

                importe(
                        xml, "PriceAmount", precioSinIgv
                );

                xml.writeEndElement(); // Price

                xml.writeEndElement(); // InvoiceLine
            }

            xml.writeEndElement(); // Invoice
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

    private void escribirSubtotalTributario(
            XMLStreamWriter xml,
            BigDecimal base,
            BigDecimal impuesto,
            String codigoTributo,
            String nombreTributo,
            String tipoTributo
    ) throws XMLStreamException {

        xml.writeStartElement(
                "cac", "TaxSubtotal", CAC_NS
        );

        importe(xml, "TaxableAmount", base);
        importe(xml, "TaxAmount", impuesto);

        xml.writeStartElement(
                "cac", "TaxCategory", CAC_NS
        );

        xml.writeStartElement(
                "cac", "TaxScheme", CAC_NS
        );

        elemento(
                xml, "cbc", CBC_NS, "ID", codigoTributo
        );

        elemento(
                xml, "cbc", CBC_NS, "Name", nombreTributo
        );

        elemento(
                xml, "cbc", CBC_NS, "TaxTypeCode", tipoTributo
        );

        xml.writeEndElement(); // TaxScheme
        xml.writeEndElement(); // TaxCategory
        xml.writeEndElement(); // TaxSubtotal
    }

    private void escribirParte(
            XMLStreamWriter xml,
            String tipo,
            String ruc,
            String razonSocial
    ) throws XMLStreamException {

        xml.writeStartElement("cac", tipo, CAC_NS);
        xml.writeStartElement("cac", "Party", CAC_NS);

        xml.writeStartElement(
                "cac", "PartyIdentification", CAC_NS
        );

        xml.writeStartElement("cbc", "ID", CBC_NS);
        xml.writeAttribute("schemeID", "6");
        xml.writeCharacters(ruc);
        xml.writeEndElement();

        xml.writeEndElement(); // PartyIdentification

        xml.writeStartElement(
                "cac", "PartyName", CAC_NS
        );

        elemento(
                xml, "cbc", CBC_NS, "Name", razonSocial
        );

        xml.writeEndElement(); // PartyName
        xml.writeEndElement(); // Party
        xml.writeEndElement(); // AccountingSupplierParty/CustomerParty
    }

    private void importe(
            XMLStreamWriter xml,
            String nombre,
            BigDecimal valor
    ) throws XMLStreamException {

        xml.writeStartElement("cbc", nombre, CBC_NS);
        xml.writeAttribute("currencyID", "PEN");
        xml.writeCharacters(valor.toPlainString());
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
