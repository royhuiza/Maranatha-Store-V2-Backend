package com.libreria.ventas.service;

import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.*;
import java.time.LocalDate;
import java.util.*;

@Service
public class ComprobanteVentaLocalService {

    private final VentaRepository repository;

    @Autowired
    private ComprobanteFirmaLocalService firmaLocalService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public ComprobanteVentaLocalService(
            VentaRepository repository
    ) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Vista consultar(Long id) {

        Venta venta = repository.findOneById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Venta no encontrada"
                        )
                );

        Vista vista = construir(venta);

        // La firma local solo corresponde a las series comerciales.

        boolean serieComercial =
                "B001".equals(venta.getSerie())
                        || "F001".equals(venta.getSerie());

        if (!serieComercial || firmaLocalService == null) {
            return vista;
        }

        // No utilizar un QR local cuando existe una emision Beta.
        if (venta.getEstadoSunat() != EstadoSunat.PENDIENTE) {
            return vista;
        }

        Optional<String> contenidoQr =
                firmaLocalService.obtenerQr(id);

        // Las ventas anteriores conservan su estado original.

        if (contenidoQr.isEmpty()
                || contenidoQr.get().isBlank()) {
            return vista;
        }

        // Comprobante firmado localmente.
        // No representa una autorizacion fiscal de SUNAT.

        return new Vista(
                "FIRMADO_LOCAL",
                false,
                vista.tipoComprobante(),
                vista.serie(),
                vista.numero(),
                vista.fechaEmision(),
                vista.tipoDocumentoCliente(),
                vista.numeroDocumentoCliente(),
                vista.clienteNombre(),
                vista.gravado(),
                vista.exonerado(),
                vista.inafecto(),
                vista.igv(),
                vista.total(),
                contenidoQr.get()
        );
    }

    // Recuperar el comprobante Beta vinculado a una venta interna.
    @Transactional(readOnly = true)
    public VistaBeta consultarBeta(Long id) {

        Venta venta = repository.findOneById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Venta no encontrada"
                        )
                );

        if (venta.getTipoComprobante() != TipoComprobante.FACTURA
                && venta.getTipoComprobante() != TipoComprobante.BOLETA) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La venta no admite comprobante Beta"
            );
        }

        List<ReferenciaBeta> referencias =
                jdbcTemplate.query(
                        """
                        SELECT serie, numero
                        FROM comprobante_beta
                        WHERE venta_id = ?
                        """,
                        (rs, rowNum) ->
                                new ReferenciaBeta(
                                        rs.getString("serie"),
                                        rs.getString("numero")
                                ),
                        id
                );

        if (referencias.isEmpty()) {

            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "La venta no tiene comprobante Beta asociado"
            );
        }

        ReferenciaBeta referencia = referencias.get(0);

        boolean factura =
                venta.getTipoComprobante() == TipoComprobante.FACTURA;

        String serieEsperada = factura ? "F9B1" : "B9B1";

        if (!serieEsperada.equals(referencia.serie())
                || referencia.numero() == null
                || !referencia.numero().matches("[0-9]{8}")) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Referencia Beta inconsistente"
            );
        }

        boolean aceptado =
                venta.getEstadoSunat() == EstadoSunat.ACEPTADO_BETA
                        && "0".equals(venta.getCodigoRespuestaSunat());

        return new VistaBeta(
                "BETA",
                false,
                venta.getId(),
                venta.getSerie(),
                venta.getNumero(),
                referencia.serie(),
                referencia.numero(),
                factura ? "01" : "03",
                venta.getFecha().toLocalDate(),
                venta.getEstadoSunat() == null
                        ? "PENDIENTE"
                        : venta.getEstadoSunat().name(),
                venta.getCodigoRespuestaSunat(),
                venta.getEstado().name(),
                venta.getIgv(),
                venta.getTotal(),
                aceptado
        );
    }

    public static Vista construir(Venta v) {

        if (v.getEstado() != EstadoVenta.EMITIDA
                || (v.getTipoComprobante() != TipoComprobante.FACTURA
                && v.getTipoComprobante() != TipoComprobante.BOLETA)) {

            throw invalido();
        }

        boolean factura =
                v.getTipoComprobante() == TipoComprobante.FACTURA;

        String documento =
                Objects.toString(
                        v.getClienteDocumento(), ""
                ).trim();

        String tipo = "0";
        String numero = "-";

        if (documento.matches("RUC [0-9]{11}")) {

            tipo = "6";
            numero = documento.substring(4);

        } else if (documento.matches("DNI [0-9]{8}")) {

            tipo = "1";
            numero = documento.substring(4);

        } else if (!documento.isEmpty()) {

            throw invalido();
        }

        if (factura && !"6".equals(tipo)) {
            throw invalido();
        }

        if (v.getFecha() == null
                || v.getSerie() == null
                || !v.getSerie().matches(
                factura
                        ? "F[A-Z0-9]{3}"
                        : "B[A-Z0-9]{3}"
        )
                || v.getNumero() == null
                || !v.getNumero().matches("[0-9]{1,8}")
                || v.getDetalles().isEmpty()) {

            throw invalido();
        }

        BigDecimal gravado =
                BigDecimal.ZERO.setScale(2);

        BigDecimal exonerado = gravado;
        BigDecimal inafecto = gravado;
        BigDecimal igv = gravado;

        for (var d : v.getDetalles()) {

            if (d.getSubtotal() == null
                    || d.getSubtotal().signum() < 0
                    || d.getAfectacionIgv() == null) {

                throw invalido();
            }

            BigDecimal importe = d.getSubtotal();

            switch (d.getAfectacionIgv()) {

                case "GRAVADO" -> {

                    BigDecimal base = importe.divide(
                            new BigDecimal("1.18"),
                            2,
                            RoundingMode.HALF_UP
                    );

                    gravado = gravado.add(base);

                    igv = igv.add(
                            importe.subtract(base)
                    );
                }

                case "EXONERADO" ->
                        exonerado = exonerado.add(importe);

                case "INAFECTO" ->
                        inafecto = inafecto.add(importe);

                default -> throw invalido();
            }
        }

        BigDecimal total = gravado
                .add(exonerado)
                .add(inafecto)
                .add(igv);

        if (v.getTotal() == null
                || total.compareTo(v.getTotal()) != 0) {

            throw invalido();
        }

        return new Vista(
                "BORRADOR_LOCAL",
                false,
                factura ? "01" : "03",
                v.getSerie(),
                v.getNumero(),
                v.getFecha().toLocalDate(),
                tipo,
                numero,
                v.getClienteNombre(),
                gravado,
                exonerado,
                inafecto,
                igv,
                total,
                null
        );
    }

    private static ResponseStatusException invalido() {

        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Venta no apta para previsualizacion tributaria"
        );
    }

    public record Vista(
            String ambiente,
            boolean validezFiscal,
            String tipoComprobante,
            String serie,
            String numero,
            LocalDate fechaEmision,
            String tipoDocumentoCliente,
            String numeroDocumentoCliente,
            String clienteNombre,
            BigDecimal gravado,
            BigDecimal exonerado,
            BigDecimal inafecto,
            BigDecimal igv,
            BigDecimal total,
            String contenidoQr
    ) {}

    private record ReferenciaBeta(
            String serie,
            String numero
    ) {}

    public record VistaBeta(
            String ambiente,
            boolean validezFiscal,
            Long ventaId,
            String serieComercial,
            String numeroComercial,
            String serie,
            String numero,
            String tipoComprobante,
            LocalDate fechaEmision,
            String estadoSunat,
            String codigo,
            String estadoVenta,
            BigDecimal igv,
            BigDecimal total,
            boolean aceptado
    ) {}
}