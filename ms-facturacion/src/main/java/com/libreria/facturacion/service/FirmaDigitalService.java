package com.libreria.facturacion.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.keyinfo.*;
import javax.xml.crypto.dsig.spec.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

@Service
public class FirmaDigitalService {

    private static final String EXT_NS =
            "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2";

    public String firmar(
            String xml,
            InputStream certificado,
            char[] password,
            String alias
    ) throws Exception {

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(certificado, password);

        PrivateKey clavePrivada =
                (PrivateKey) keyStore.getKey(alias, password);

        X509Certificate certificadoX509 =
                (X509Certificate) keyStore.getCertificate(alias);

        if (clavePrivada == null || certificadoX509 == null) {
            throw new IllegalStateException(
                    "Certificado o clave privada no encontrados");
        }

        certificadoX509.checkValidity();

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document documento = factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));

        Node extension = documento
                .getElementsByTagNameNS(EXT_NS, "ExtensionContent")
                .item(0);

        if (extension == null) {
            throw new IllegalStateException(
                    "No existe ExtensionContent en el XML");
        }

        XMLSignatureFactory firmaFactory =
                XMLSignatureFactory.getInstance("DOM");

        Reference referencia = firmaFactory.newReference(
                "",
                firmaFactory.newDigestMethod(
                        DigestMethod.SHA256, null),
                List.of(
                        firmaFactory.newTransform(
                                Transform.ENVELOPED,
                                (TransformParameterSpec) null)
                ),
                null,
                null
        );

        SignedInfo signedInfo = firmaFactory.newSignedInfo(
                firmaFactory.newCanonicalizationMethod(
                        CanonicalizationMethod.INCLUSIVE,
                        (C14NMethodParameterSpec) null),
                firmaFactory.newSignatureMethod(
                        SignatureMethod.RSA_SHA256,
                        null),
                List.of(referencia)
        );

        KeyInfoFactory keyInfoFactory =
                firmaFactory.getKeyInfoFactory();

        X509Data x509Data = keyInfoFactory.newX509Data(
                List.of(certificadoX509)
        );

        KeyInfo keyInfo = keyInfoFactory.newKeyInfo(
                List.of(x509Data)
        );

        DOMSignContext contexto =
                new DOMSignContext(clavePrivada, extension);

        contexto.setDefaultNamespacePrefix("ds");

        XMLSignature firma = firmaFactory.newXMLSignature(
                signedInfo,
                keyInfo,
                null,
                "SignatureSP",
                null
        );

        firma.sign(contexto);

        TransformerFactory transformerFactory =
                TransformerFactory.newInstance();

        transformerFactory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING, true);

        Transformer transformer =
                transformerFactory.newTransformer();

        transformer.setOutputProperty(
                OutputKeys.ENCODING, "UTF-8");

        transformer.setOutputProperty(
                OutputKeys.INDENT, "no");

        StringWriter salida = new StringWriter();

        transformer.transform(
                new DOMSource(documento),
                new StreamResult(salida));

        return salida.toString();
    }
}
