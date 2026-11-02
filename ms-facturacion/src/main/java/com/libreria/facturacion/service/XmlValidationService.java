package com.libreria.facturacion.service;

import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

@Service
public class XmlValidationService {

    public void validarXml(String xml, Path rutaXsd) {

        if (xml == null || xml.isBlank()) {
            throw new IllegalArgumentException(
                    "El XML no puede estar vacio"
            );
        }

        if (rutaXsd == null) {
            throw new IllegalArgumentException(
                    "La ruta del esquema XSD es obligatoria"
            );
        }

        try {
            SchemaFactory factory = SchemaFactory.newInstance(
                    XMLConstants.W3C_XML_SCHEMA_NS_URI
            );

            factory.setFeature(
                    XMLConstants.FEATURE_SECURE_PROCESSING,
                    true
            );

            factory.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            factory.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    "file"
            );

            Schema schema = factory.newSchema(rutaXsd.toFile());

            Validator validator = schema.newValidator();

            validator.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            validator.setProperty(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            validator.validate(
                    new StreamSource(new StringReader(xml))
            );

        } catch (SAXException | IOException ex) {
            throw new IllegalArgumentException(
                    "El XML no cumple con el esquema XSD: "
                            + ex.getMessage(),
                    ex
            );
        }
    }
}
