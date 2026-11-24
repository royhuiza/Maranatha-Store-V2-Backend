package com.libreria.facturacion.service;
import org.springframework.stereotype.Service;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Base64;

@Service
public class QrFiscalService {
    // Extrae datos del XML firmado generado por nuestro servicio.
    // No verifica por si solo la autenticidad ni la aceptacion SUNAT.
    public DatosQr extraer(String xml) throws Exception {
        var f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true);
        f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        var doc = f.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        if (!"Invoice".equals(doc.getDocumentElement().getLocalName())) throw new IllegalArgumentException("XML no admitido");
        var xp = XPathFactory.newInstance().newXPath();
        String r = "/*[local-name()='Invoice']";
        String id = xp.evaluate(r+"/*[local-name()='ID']",doc);
        String tipo = xp.evaluate(r+"/*[local-name()='InvoiceTypeCode']",doc);
        if (!id.matches("[FB][A-Z0-9]{3}-[0-9]{1,8}") || !("01".equals(tipo) && id.startsWith("F") || "03".equals(tipo) && id.startsWith("B"))) throw new IllegalArgumentException("Identificador invalido");
        String emisor = r+"/*[local-name()='AccountingSupplierParty']/*[local-name()='Party']/*[local-name()='PartyIdentification']/*[local-name()='ID']";
        String cliente = r+"/*[local-name()='AccountingCustomerParty']/*[local-name()='Party']/*[local-name()='PartyIdentification']/*[local-name()='ID']";
        String ruc = xp.evaluate(emisor,doc);
        String dni = xp.evaluate(cliente,doc);
        String tipoDoc = xp.evaluate(cliente+"/@schemeID",doc);
        String igv = importe(xp.evaluate(r+"/*[local-name()='TaxTotal']/*[local-name()='TaxAmount']",doc));
        String total = importe(xp.evaluate(r+"/*[local-name()='LegalMonetaryTotal']/*[local-name()='PayableAmount']",doc));
        String fecha = java.time.LocalDate.parse(xp.evaluate(r+"/*[local-name()='IssueDate']",doc)).toString();
        var digestos = doc.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#","DigestValue");
        if (digestos.getLength()!=1) throw new IllegalArgumentException("Se requiere XML firmado");
        String hash = digestos.item(0).getTextContent().trim();
        if (Base64.getDecoder().decode(hash).length!=32) throw new IllegalArgumentException("Resumen SHA256 invalido");
        if (!ruc.matches("[0-9]{11}") || !tipoDoc.matches("[01467]") || dni.isBlank() || dni.contains("|")) throw new IllegalArgumentException("Identidad invalida");
        String[] n = id.split("-");
        String contenido = String.join("|",ruc,tipo,n[0],n[1],igv,total,fecha,tipoDoc,dni,hash);
        return new DatosQr("PRUEBA",id,tipo,igv,total,hash,contenido);
    }
    private String importe(String valor) { return new BigDecimal(valor).setScale(2,RoundingMode.UNNECESSARY).toPlainString(); }
    public record DatosQr(String ambiente,String comprobante,String tipoComprobante,String igv,String total,String hash,String contenidoQr) {}
}
