
package com.libreria.facturacion.service;

import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.crypto.dsig.*;
import javax.xml.crypto.dsig.dom.DOMSignContext;
import javax.xml.crypto.dsig.dom.DOMValidateContext;
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

        Document documento = leerXml(xml);

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

        String xmlFirmado = salida.toString();

        // VERIFICAR LA FIRMA DESPUES DE SERIALIZAR EL XML.

        verificarFirma(xmlFirmado, certificadoX509);

        return xmlFirmado;
    }

    // VERIFICACION CRIPTOGRAFICA DE LA FIRMA DIGITAL.

    public void verificarFirma(
            String xmlFirmado,
            X509Certificate certificadoX509
    ) throws Exception {

        certificadoX509.checkValidity();

        Document documento = leerXml(xmlFirmado);

        NodeList firmas = documento.getElementsByTagNameNS(
                XMLSignature.XMLNS,
                "Signature"
        );

        if (firmas.getLength() != 1) {

            throw new IllegalStateException(
                    "El XML debe contener una unica firma digital"
            );
        }

        Node nodoFirma = firmas.item(0);

        Node padre = nodoFirma.getParentNode();

        if (padre == null
                || !EXT_NS.equals(padre.getNamespaceURI())
                || !"ExtensionContent".equals(padre.getLocalName())) {

            throw new IllegalStateException(
                    "Ubicacion de firma digital invalida"
            );
        }

        DOMValidateContext contexto =
                new DOMValidateContext(
                        certificadoX509.getPublicKey(),
                        nodoFirma
                );

        contexto.setProperty(
                "org.jcp.xml.dsig.secureValidation",
                Boolean.TRUE
        );

        XMLSignatureFactory firmaFactory =
                XMLSignatureFactory.getInstance("DOM");

        XMLSignature firma =
                firmaFactory.unmarshalXMLSignature(contexto);

        SignedInfo informacion = firma.getSignedInfo();

        if (!SignatureMethod.RSA_SHA256.equals(
                informacion.getSignatureMethod().getAlgorithm())
                || !CanonicalizationMethod.INCLUSIVE.equals(
                informacion.getCanonicalizationMethod().getAlgorithm())
                || informacion.getReferences().size() != 1) {

            throw new IllegalStateException(
                    "Configuracion criptografica de firma invalida"
            );
        }

        Reference referencia =
                informacion.getReferences().get(0);

        if (!"".equals(referencia.getURI())
                || !DigestMethod.SHA256.equals(
                referencia.getDigestMethod().getAlgorithm())
                || referencia.getTransforms().size() != 1
                || !Transform.ENVELOPED.equals(
                referencia.getTransforms().get(0).getAlgorithm())) {

            throw new IllegalStateException(
                    "Referencia criptografica del XML invalida"
            );
        }

        if (!firma.validate(contexto)) {

            throw new IllegalStateException(
                    "La firma digital del XML no es valida"
            );
        }
    }

    // LECTURA SEGURA DE DOCUMENTOS XML.

    private Document leerXml(
            String xml
    ) throws Exception {

        DocumentBuilderFactory factory =
                DocumentBuilderFactory.newInstance();

        factory.setNamespaceAware(true);

        factory.setFeature(
                XMLConstants.FEATURE_SECURE_PROCESSING,
                true
        );

        factory.setFeature(
                "http://apache.org/xml/features/disallow-doctype-decl",
                true
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_DTD,
                ""
        );

        factory.setAttribute(
                XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                ""
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        return factory.newDocumentBuilder()
                .parse(new InputSource(new StringReader(xml)));
    }
}
