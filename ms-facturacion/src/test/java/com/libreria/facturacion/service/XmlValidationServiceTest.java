
package com.libreria.facturacion.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class XmlValidationServiceTest {

    private final XmlValidationService service =
            new XmlValidationService();

    @TempDir
    Path carpetaTemporal;

    @Test
    void debeAceptarXmlQueCumpleElEsquema() throws Exception {

        Path xsd = crearEsquemaPrueba();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Comprobante>
                    <Numero>F001-00000001</Numero>
                </Comprobante>
                """;

        assertDoesNotThrow(() ->
                service.validarXml(xml, xsd)
        );
    }

    @Test
    void debeRechazarXmlQueNoCumpleElEsquema() throws Exception {

        Path xsd = crearEsquemaPrueba();

        String xml = """
                <?xml version="1.0" encoding="UTF-8"?>
                <Comprobante>
                    <CampoIncorrecto>123</CampoIncorrecto>
                </Comprobante>
                """;

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validarXml(xml, xsd)
        );
    }

    @Test
    void debeRechazarXmlVacio() throws Exception {

        Path xsd = crearEsquemaPrueba();

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validarXml("", xsd)
        );
    }

    @Test
    void debeRechazarRutaXsdNula() {

        assertThrows(
                IllegalArgumentException.class,
                () -> service.validarXml(
                        "<Comprobante/>",
                        null
                )
        );
    }

    private Path crearEsquemaPrueba() throws Exception {

        String esquema = """
                <?xml version="1.0" encoding="UTF-8"?>
                <xs:schema
                    xmlns:xs="http://www.w3.org/2001/XMLSchema">

                    <xs:element name="Comprobante">
                        <xs:complexType>
                            <xs:sequence>
                                <xs:element
                                    name="Numero"
                                    type="xs:string"/>
                            </xs:sequence>
                        </xs:complexType>
                    </xs:element>

                </xs:schema>
                """;

        Path ruta = carpetaTemporal.resolve(
                "comprobante-prueba.xsd"
        );

        Files.writeString(ruta, esquema);

        return ruta;
    }
}
