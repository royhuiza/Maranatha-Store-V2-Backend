package com.libreria.facturacion.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Service
public class ArchivoFiscalService {

    @Value("${sunat.archivos.directorio:./archivos-fiscales}")
    private String directorio;

    public Path reservarComprobante(String nombre)
            throws Exception {

        if (nombre == null ||
                !nombre.matches("[0-9]{11}-0[13]-[FB][A-Z0-9]{3}-[0-9]{1,8}")) {

            throw new IllegalArgumentException(
                    "Identificador fiscal invalido");
        }

        Path raiz = Path.of(directorio)
                .toAbsolutePath()
                .normalize()
                .resolve("beta");

        Files.createDirectories(raiz);

        Path carpeta = raiz.resolve(nombre);

        // Impide sobrescribir o reenviar automaticamente
        // un comprobante que ya fue procesado.
        return Files.createDirectory(carpeta);
    }

    public void guardarXmlFirmado(
            Path carpeta,
            String xml
    ) throws Exception {

        guardar(carpeta, "factura.xml", xml);
    }

    public void guardarRespuestaSoap(
            Path carpeta,
            String respuesta
    ) throws Exception {

        guardar(carpeta, "respuesta-soap.xml", respuesta);
    }

    public void guardarCdr(
            Path carpeta,
            String cdr
    ) throws Exception {

        guardar(carpeta, "cdr.xml", cdr);
    }

    private void guardar(
            Path carpeta,
            String nombre,
            String contenido
    ) throws Exception {

        if (contenido == null || contenido.isBlank()) {
            throw new IllegalArgumentException(
                    "El contenido no puede estar vacio");
        }

        Files.writeString(
                carpeta.resolve(nombre),
                contenido,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW,
                StandardOpenOption.WRITE
        );
    }
}
