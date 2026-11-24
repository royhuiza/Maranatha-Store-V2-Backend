package com.libreria.facturacion.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SunatBetaEnvioTest {

    @Test
    void enviarFacturaFirmadaABeta() throws Exception {

        Path modulo = Path.of("").toAbsolutePath();

        if (!Files.isDirectory(
                modulo.resolve("src/main/resources/xsd"))) {

            modulo = modulo.resolve("ms-facturacion");
        }

        Path archivoXml = modulo.resolve(
                "target/factura-beta.xml");

        assertTrue(
                Files.exists(archivoXml),
                "No se encontro el XML firmado");

        String xmlFirmado = Files.readString(
                archivoXml,
                StandardCharsets.UTF_8);

        assertTrue(
                xmlFirmado.contains("SignatureValue"),
                "El XML no contiene una firma digital");

        SunatSoapClient cliente =
                new SunatSoapClient();

        ReflectionTestUtils.setField(
                cliente,
                "habilitado",
                true
        );

        CdrService procesador =
                new CdrService();

        System.out.println(
                "ENVIANDO COMPROBANTE DE PRUEBA A SUNAT BETA");

        String respuesta = cliente.enviarFactura(
                "20123456786",
                "20123456786-01-F001-00000001",
                xmlFirmado
        );

        Path archivoRespuesta = modulo.resolve(
                "target/respuesta-sunat-beta.xml");

        Files.writeString(
                archivoRespuesta,
                respuesta,
                StandardCharsets.UTF_8);

        var resultado = procesador.procesar(respuesta);

        System.out.println("==============================");
        System.out.println("AMBIENTE: SUNAT BETA");
        System.out.println("ESTADO: " + resultado.estado());
        System.out.println("CODIGO: " + resultado.codigo());
        System.out.println("MENSAJE: " + resultado.mensaje());
        System.out.println("==============================");

        if (resultado.xmlCdr() != null) {

            Path archivoCdr = modulo.resolve(
                    "target/cdr-beta.xml");

            Files.writeString(
                    archivoCdr,
                    resultado.xmlCdr(),
                    StandardCharsets.UTF_8);

            System.out.println("CDR GUARDADO: " + archivoCdr);
        }

        assertEquals(
                "ACEPTADA_BETA",
                resultado.estado(),
                "SUNAT BETA: " + resultado.mensaje()
        );
    }
}