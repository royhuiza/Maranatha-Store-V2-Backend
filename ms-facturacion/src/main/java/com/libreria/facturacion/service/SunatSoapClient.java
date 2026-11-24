package com.libreria.facturacion.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class SunatSoapClient {

    private static final String URL_BETA =
            "https://e-beta.sunat.gob.pe/ol-ti-itcpfegem-beta/billService";

    @Value("${sunat.beta.enabled:false}")
    private boolean habilitado;

    private final HttpClient cliente = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public String enviarFactura(
            String rucEmisor,
            String nombreArchivo,
            String xmlFirmado
    ) throws Exception {

        if (!habilitado) {
            throw new IllegalStateException(
                    "El envio SUNAT Beta no esta habilitado");
        }

        if (!rucEmisor.matches("[0-9]{11}")) {
            throw new IllegalArgumentException("RUC invalido");
        }

        if (!nombreArchivo.matches("[0-9A-Z-]+")) {
            throw new IllegalArgumentException(
                    "Nombre de archivo invalido");
        }

        byte[] zip = comprimir(nombreArchivo, xmlFirmado);

        String contenido = Base64.getEncoder()
                .encodeToString(zip);

        String usuario = rucEmisor + "MODDATOS";

        String soap = """
                <?xml version="1.0" encoding="UTF-8"?>
                <soapenv:Envelope
                  xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                  xmlns:ser="http://service.sunat.gob.pe"
                  xmlns:wsse="http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd">
                    <soapenv:Header>
                        <wsse:Security>
                            <wsse:UsernameToken>
                                <wsse:Username>%s</wsse:Username>
                                <wsse:Password>MODDATOS</wsse:Password>
                            </wsse:UsernameToken>
                        </wsse:Security>
                    </soapenv:Header>
                    <soapenv:Body>
                        <ser:sendBill>
                            <fileName>%s.zip</fileName>
                            <contentFile>%s</contentFile>
                        </ser:sendBill>
                    </soapenv:Body>
                </soapenv:Envelope>
                """.formatted(usuario, nombreArchivo, contenido);

        HttpRequest solicitud = HttpRequest.newBuilder()
                .uri(URI.create(URL_BETA))
                .timeout(Duration.ofSeconds(60))
                .header("Content-Type", "text/xml; charset=UTF-8")
                .header("SOAPAction", "urn:sendBill")
                .POST(HttpRequest.BodyPublishers.ofString(
                        soap, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> respuesta = cliente.send(
                solicitud,
                HttpResponse.BodyHandlers.ofString());

        return respuesta.body();
    }

    private byte[] comprimir(
            String nombre,
            String xml
    ) throws Exception {

        ByteArrayOutputStream salida =
                new ByteArrayOutputStream();

        try (ZipOutputStream zip =
                     new ZipOutputStream(salida)) {

            zip.putNextEntry(
                    new ZipEntry(nombre + ".xml"));

            zip.write(xml.getBytes(StandardCharsets.UTF_8));

            zip.closeEntry();
        }

        return salida.toByteArray();
    }
}
