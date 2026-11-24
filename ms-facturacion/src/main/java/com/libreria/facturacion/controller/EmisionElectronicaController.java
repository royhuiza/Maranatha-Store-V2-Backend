package com.libreria.facturacion.controller;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.service.CdrService;
import com.libreria.facturacion.service.EmisionElectronicaService;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;
import java.nio.file.Files;
import java.nio.file.FileAlreadyExistsException;
import java.util.Arrays;

@RestController
@RequestMapping("/api/facturacion/emision")
public class EmisionElectronicaController {

    private final EmisionElectronicaService emisionService;

    @Value("${sunat.beta.enabled:false}")
    private boolean habilitado;

    @Value("${sunat.certificado.ruta:}")
    private String certificado;

    @Value("${sunat.certificado.password:}")
    private String password;

    @Value("${sunat.xsd.ruta:}")
    private String xsd;

    public EmisionElectronicaController(
            EmisionElectronicaService emisionService
    ) {
        this.emisionService = emisionService;
    }

    @PostMapping("/beta")
    public ResultadoEmision emitirBeta(
            @Valid @RequestBody FacturaPruebaRequest factura
    ) {

        if (!habilitado) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Emision Beta deshabilitada");
        }

        if (certificado.isBlank()
                || password.isBlank()
                || xsd.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Configuracion fiscal incompleta");
        }

        // Solo datos sinteticos del escenario de pruebas.
        if (!"20123456786".equals(factura.rucEmisor())
                || !"20123456786".equals(factura.rucCliente())
                || !"01".equals(factura.tipoComprobante())
                || !"MARANATHA BETA - PRUEBA".equals(
                factura.razonSocialEmisor())
                || !"EMPRESA FICTICIA DE PRUEBA".equals(
                factura.razonSocialCliente())
                || factura.detalles().size() != 1
                || !"Producto de prueba".equals(
                factura.detalles().get(0).descripcion())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se permiten facturas sinteticas Beta");
        }

        Path rutaCertificado = Path.of(certificado);
        Path rutaXsd = Path.of(xsd);

        if (!Files.isRegularFile(rutaCertificado)
                || !Files.isRegularFile(rutaXsd)) {

            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Recursos fiscales no disponibles");
        }

        char[] clave = password.toCharArray();

        try {

            CdrService.ResultadoCdr resultado =
                    emisionService.emitirBeta(
                            factura,
                            rutaCertificado,
                            clave,
                            rutaXsd
                    );

            return new ResultadoEmision(
                    "BETA",
                    resultado.estado(),
                    resultado.codigo(),
                    resultado.mensaje(),
                    factura.serie(),
                    factura.numero()
            );

        } catch (FileAlreadyExistsException ex) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Comprobante existente. Requiere revision");

        } catch (Exception ex) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "No se pudo confirmar la emision Beta. "
                            + "Verifique el estado antes de reintentar");

        } finally {
            Arrays.fill(clave, '\0');
        }
    }

    public record ResultadoEmision(
            String ambiente,
            String estado,
            String codigo,
            String mensaje,
            String serie,
            String numero
    ) {}
}
