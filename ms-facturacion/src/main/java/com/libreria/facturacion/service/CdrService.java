package com.libreria.facturacion.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipEntry;

@Service
public class CdrService {

    private static final int LIMITE_XML = 5_000_000;

    public ResultadoCdr procesar(String respuestaSoap)
            throws Exception {

        Document soap = leerXml(respuestaSoap);

        String fault = obtenerTexto(soap, "faultstring");

        if (!fault.isBlank()) {
            return new ResultadoCdr(
                    "ERROR_SOAP",
                    obtenerTexto(soap, "faultcode"),
                    fault,
                    null
            );
        }

        String contenido =
                obtenerTexto(soap, "applicationResponse");

        if (contenido.isBlank()) {
            throw new IllegalStateException(
                    "SUNAT no devolvio una constancia CDR");
        }

        byte[] zip = Base64.getMimeDecoder()
                .decode(contenido);

        String xmlCdr = extraerXml(zip);

        Document documento = leerXml(xmlCdr);

        String codigo =
                obtenerTexto(documento, "ResponseCode");

        String descripcion =
                obtenerTexto(documento, "Description");

        if (codigo.isBlank()) {
            throw new IllegalStateException(
                    "CDR sin codigo de respuesta");
        }

        String estado = "0".equals(codigo)
                ? "ACEPTADA_BETA"
                : "REVISAR_CDR_BETA";

        return new ResultadoCdr(
                estado,
                codigo,
                descripcion,
                xmlCdr
        );
    }

    private String extraerXml(byte[] archivo)
            throws Exception {

        try (ZipInputStream zip = new ZipInputStream(
                new ByteArrayInputStream(archivo))) {

            ZipEntry entrada;

            while ((entrada = zip.getNextEntry()) != null) {

                if (!entrada.isDirectory()
                        && entrada.getName()
                        .toLowerCase().endsWith(".xml")) {

                    byte[] datos =
                            zip.readNBytes(LIMITE_XML + 1);

                    if (datos.length > LIMITE_XML) {
                        throw new IllegalStateException(
                                "CDR demasiado grande");
                    }

                    return new String(
                            datos, StandardCharsets.UTF_8);
                }
            }
        }

        throw new IllegalStateException(
                "El ZIP no contiene un CDR XML");
    }

    private Document leerXml(String xml)
            throws Exception {

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

        return factory.newDocumentBuilder()
                .parse(new InputSource(
                        new StringReader(xml)));
    }

    private String obtenerTexto(
            Document documento,
            String etiqueta
    ) {

        NodeList nodos =
                documento.getElementsByTagNameNS(
                        "*", etiqueta);

        return nodos.getLength() > 0
                ? nodos.item(0).getTextContent().trim()
                : "";
    }

    public record ResultadoCdr(
            String estado,
            String codigo,
            String mensaje,
            String xmlCdr
    ) {}
}
