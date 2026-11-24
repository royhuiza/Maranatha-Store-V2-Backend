package com.libreria.facturacion.controller;

import com.libreria.facturacion.service.QrFiscalService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.crypto.dsig.XMLSignatureFactory;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
import java.security.cert.CertificateFactory;
import java.io.ByteArrayInputStream;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** Consulta evidencia de laboratorio existente. No emite ni modifica ventas. */
@RestController
@RequestMapping("/api/facturacion/laboratorio/comprobantes")
public class ComprobanteBetaArchivadoController {
    private static final String RUC = "20123456786";
    private static final String CBC = "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2";
    private static final String CAC = "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2";
    private static final String DS = "http://www.w3.org/2000/09/xmldsig#";
    private final QrFiscalService qr;
    private final Path raiz;

    public ComprobanteBetaArchivadoController(QrFiscalService qr,
            @Value("${sunat.archivos.directorio:./archivos-fiscales}") String directorio) {
        this.qr = qr;
        this.raiz = Path.of(directorio).toAbsolutePath().normalize();
    }

    @GetMapping("/{numero}")
    public Vista consultar(@PathVariable("numero") String numero) {
        if (!numero.matches("[0-9]{8}")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Correlativo de ocho digitos requerido");
        String id = "F9B1-" + numero;
        Path carpeta = raiz.resolve("beta").resolve(RUC + "-01-" + id);
        Path factura = carpeta.resolve("factura.xml"), cdr = carpeta.resolve("cdr.xml");
        if (!Files.isRegularFile(factura) || !Files.isRegularFile(cdr))
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Evidencia Beta no disponible");
        try {
            if (!factura.toRealPath().startsWith(raiz.toRealPath()) || !cdr.toRealPath().startsWith(raiz.toRealPath())) throw new IllegalArgumentException();
            String xml = leer(factura);
            Document doc = parse(xml);
            validarFirmaLocal(doc);
            QrFiscalService.DatosQr datos = qr.extraer(xml);
            String[] campos = datos.contenidoQr().split("\\|", -1);
            if (campos.length != 10 || !RUC.equals(campos[0]) || !RUC.equals(campos[8]) || !"01".equals(campos[1]) || !id.equals(datos.comprobante())) throw new IllegalArgumentException();
            Document respuesta = parse(leer(cdr));
            Element app = respuesta.getDocumentElement();
            if (!"ApplicationResponse".equals(app.getLocalName()) || !"urn:oasis:names:specification:ubl:schema:xsd:ApplicationResponse-2".equals(app.getNamespaceURI())) throw new IllegalArgumentException();
            Element dr = unico(app,CAC,"DocumentResponse");
            Element resp = unico(dr,CAC,"Response");
            String codigo = texto(resp,CBC,"ResponseCode");
            if (!id.equals(texto(resp,CBC,"ReferenceID")) || !id.equals(texto(unico(dr,CAC,"DocumentReference"),CBC,"ID")) || !"0".equals(codigo)) throw new IllegalArgumentException();
            if (!RUC.equals(texto(unico(unico(app,CAC,"ReceiverParty"),CAC,"PartyIdentification"),CBC,"ID"))) throw new IllegalArgumentException();
            if (!"20131312955".equals(texto(unico(unico(app,CAC,"SenderParty"),CAC,"PartyIdentification"),CBC,"ID"))) throw new IllegalArgumentException();
            Element invoice = doc.getDocumentElement();
            Element cliente = unico(unico(invoice,CAC,"AccountingCustomerParty"),CAC,"Party");
            String nombreCliente = texto(unico(cliente,CAC,"PartyLegalEntity"),CBC,"RegistrationName");
            String subtotal = texto(unico(invoice,CAC,"LegalMonetaryTotal"),CBC,"LineExtensionAmount");
            var detalle = new java.util.ArrayList<Linea>();
            for (var n=invoice.getFirstChild(); n!=null; n=n.getNextSibling()) {
                if (n instanceof Element e && CAC.equals(e.getNamespaceURI()) && "InvoiceLine".equals(e.getLocalName())) {
                    String descripcion = texto(unico(e,CAC,"Item"),CBC,"Description");
                    String cantidad = texto(e,CBC,"InvoicedQuantity");
                    String precio = texto(unico(unico(e,CAC,"PricingReference"),CAC,"AlternativeConditionPrice"),CBC,"PriceAmount");
                    String neto = texto(e,CBC,"LineExtensionAmount");
                    String impuesto = texto(unico(e,CAC,"TaxTotal"),CBC,"TaxAmount");
                    String importe = new java.math.BigDecimal(neto).add(new java.math.BigDecimal(impuesto)).toPlainString();
                    detalle.add(new Linea(descripcion,cantidad,precio,importe));
                }
            }
            if (detalle.isEmpty()) throw new IllegalArgumentException();
            return new Vista("BETA",false,id,"ACEPTADO_BETA",codigo,datos.igv(),datos.total(),campos[6],datos.contenidoQr(),subtotal,nombreCliente,java.util.List.copyOf(detalle));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Evidencia Beta inconsistente o no verificable");
        }
    }
    private static String leer(Path p) throws Exception {
        if (Files.size(p) > 1024 * 1024) throw new IllegalArgumentException();
        return Files.readString(p, StandardCharsets.UTF_8);
    }
    private static Document parse(String xml) throws Exception {
        var f = DocumentBuilderFactory.newInstance();
        f.setNamespaceAware(true); f.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING,true);
        f.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD,""); f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA,"");
        return f.newDocumentBuilder().parse(new org.xml.sax.InputSource(new java.io.StringReader(xml)));
    }
    private static Element unico(Element parent,String ns,String nombre) {
        Element found=null;
        for(var n=parent.getFirstChild();n!=null;n=n.getNextSibling()) {
            if(n instanceof Element e && ns.equals(e.getNamespaceURI()) && nombre.equals(e.getLocalName())) {
                if(found!=null) throw new IllegalArgumentException(); found=e;
            }
        }
        if(found==null) throw new IllegalArgumentException(); return found;
    }
    private static String texto(Element e,String ns,String n) { return unico(e,ns,n).getTextContent().trim(); }
    private static void validarFirmaLocal(Document doc) throws Exception {
        var firmas=doc.getElementsByTagNameNS(DS,"Signature");
        var certs=doc.getElementsByTagNameNS(DS,"X509Certificate");
        if(firmas.getLength()!=1 || certs.getLength()!=1) throw new IllegalArgumentException();
        var cert=CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(Base64.getMimeDecoder().decode(certs.item(0).getTextContent())));
        var ctx=new DOMValidateContext(cert.getPublicKey(),firmas.item(0));
        ctx.setProperty("org.jcp.xml.dsig.secureValidation",true);
        var factory=XMLSignatureFactory.getInstance("DOM");
        var firma=factory.unmarshalXMLSignature(ctx);
        var refs=firma.getSignedInfo().getReferences();
        if(refs.size()!=1 || !"".equals(refs.get(0).getURI())) throw new IllegalArgumentException();
        // Solo referencias internas, sin confianza de produccion ni validacion de firma CDR Beta.
        var original=factory.getURIDereferencer();
        ctx.setURIDereferencer((ref,c)-> { if(!"".equals(ref.getURI())) throw new javax.xml.crypto.URIReferenceException("Referencia externa"); return original.dereference(ref,c); });
        if(!firma.validate(ctx)) throw new IllegalArgumentException();
    }
    public record Vista(String ambiente,boolean validezFiscal,String comprobante,String estadoSunat,String codigo,String igv,String total,String fecha,String contenidoQr,String subtotal,String cliente,java.util.List<Linea> detalles) {}
    public record Linea(String descripcion,String cantidad,String precioUnitario,String importe) {}
}
