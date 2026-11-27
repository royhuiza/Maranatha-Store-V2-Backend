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

    @Value("${sunat.archivos.directorio:./archivos-fiscales}")
    private String directorio;

    public EmisionElectronicaController(
            EmisionElectronicaService emisionService
    ) {
        this.emisionService = emisionService;
    }

    @PostMapping("/beta")
    public ResultadoEmision emitirBeta(
            @Valid @RequestBody FacturaPruebaRequest factura
    ) {

        return emitir(factura, false);
    }

    ResultadoEmision emitirInternaBeta(
            FacturaPruebaRequest factura
    ) {
        return emitir(factura, true);
    }

    private ResultadoEmision emitir(
            FacturaPruebaRequest factura,
            boolean interno
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

        // Compatibilidad con las pruebas originales.

        boolean legacy = factura.detalles() != null
                && factura.detalles().size() == 1
                && "Producto de prueba".equals(
                factura.detalles().get(0).descripcion());

        // Catalogo original del laboratorio Beta.

        boolean catalogoLaboratorio = interno
                && factura.detalles() != null
                && !factura.detalles().isEmpty()
                && factura.detalles().size() <= 50
                && factura.detalles().stream()
                .map(FacturaPruebaRequest.Detalle::descripcion)
                .distinct().count() == factura.detalles().size()
                && factura.detalles().stream().allMatch(d ->
                com.libreria.facturacion.service.CatalogoBeta.valida(
                        d.descripcion(),
                        d.precioUnitario(),
                        d.cantidad(),
                        d.afectacionIgv() == null
                                ? null
                                : d.afectacionIgv().name()
                ));

        // Catalogo sintetico generado desde el flujo integrado
        // de ventas de Maranatha Store.
        // Solo se permite mediante autenticacion interna.

        boolean catalogoIntegrado = interno
                && factura.detalles() != null
                && !factura.detalles().isEmpty()
                && factura.detalles().size() <= 50
                && factura.detalles().stream()
                .map(FacturaPruebaRequest.Detalle::descripcion)
                .distinct().count() == factura.detalles().size()
                && factura.detalles().stream().allMatch(d ->
                d.descripcion() != null
                        && d.descripcion().matches(
                        "Producto de prueba [0-9]{1,3}")
                        && d.precioUnitario() != null
                        && d.precioUnitario().signum() > 0
                        && d.cantidad() != null
                        && d.cantidad() > 0
                        && d.afectacionIgv() != null
        );

        boolean catalogo =
                catalogoLaboratorio || catalogoIntegrado;

        boolean boleta = interno
                && catalogo
                && "03".equals(factura.tipoComprobante())
                && "B9B1".equals(factura.serie());

        // Solo datos sinteticos del escenario de pruebas.
        // No se permite enviar datos personales reales.

        if (!"20123456786".equals(factura.rucEmisor())
                || !"20123456786".equals(factura.rucCliente())
                || (!"01".equals(factura.tipoComprobante()) && !boleta)
                || !"MARANATHA BETA - PRUEBA".equals(
                factura.razonSocialEmisor())
                || !"EMPRESA FICTICIA DE PRUEBA".equals(
                factura.razonSocialCliente())
                || (!legacy && !catalogo)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Solo se permiten facturas sinteticas Beta");
        }

        // Validar series, correlativos e identidad de prueba.

        if (catalogo
                && (!(boleta ? "B9B1" : "F9B1").equals(factura.serie())
                || !"6".equals(factura.tipoDocumentoCliente())
                || factura.numero() == null
                || !factura.numero().matches("[0-9]{8}")
                || factura.direccionEmisor() != null)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Identidad de laboratorio invalida");
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

            // Generar XML, firmar, validar y enviar a SUNAT Beta.

            CdrService.ResultadoCdr resultado =
                    emisionService.emitirBeta(
                            factura,
                            rutaCertificado,
                            clave,
                            rutaXsd
                    );

            // No confirmar codigo 0 hasta verificar XML firmado,
            // CDR y correlativo archivados.

            if ("0".equals(resultado.codigo())
                    && (catalogo || "F9B1".equals(factura.serie()))) {

                var evidencia =
                        new ComprobanteBetaArchivadoController(
                                new com.libreria.facturacion.service.QrFiscalService(),
                                directorio
                        );

                var vista = evidencia.consultarSerie(
                        factura.serie(),
                        factura.numero()
                );

                if (!"ACEPTADO_BETA".equals(vista.estadoSunat())
                        || vista.validezFiscal()) {

                    throw new IllegalStateException(
                            "Evidencia Beta no confirmada");
                }
            }

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