package com.libreria.ventas.service;

import com.libreria.ventas.client.FacturacionClient;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.EstadoSunat;
import com.libreria.ventas.model.TipoComprobante;
import com.libreria.ventas.model.Venta;
import com.libreria.ventas.repository.VentaRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class VentaFirmaAutomaticaService {

    private static final Logger log =
            LoggerFactory.getLogger(
                    VentaFirmaAutomaticaService.class
            );

    private final FacturacionClient facturacionClient;
    private final ComprobanteFirmaLocalService firmaLocalService;
    private final VentaRepository ventaRepository;

    @Autowired
    private VentaFiscalService ventaFiscalService;

    @Value("${facturacion.local.enabled:false}")
    private boolean habilitado;

    @Value("${facturacion.beta.automatica.enabled:false}")
    private boolean betaHabilitado;

    public VentaFirmaAutomaticaService(
            FacturacionClient facturacionClient,
            ComprobanteFirmaLocalService firmaLocalService,
            VentaRepository ventaRepository
    ) {
        this.facturacionClient = facturacionClient;
        this.firmaLocalService = firmaLocalService;
        this.ventaRepository = ventaRepository;
    }

    // Determina si corresponde emitir en SUNAT Beta.
    public boolean isBetaHabilitado() {
        return betaHabilitado;
    }

    // Emision automatica en SUNAT Beta.
    // No mantiene una transaccion abierta durante el envio remoto.
    public void emitirVentaBeta(Long ventaId) {

        if (!betaHabilitado || ventaId == null) {
            return;
        }

        Venta venta = ventaRepository.findOneById(ventaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venta no encontrada"
                        )
                );

        if (venta.getEstado() != EstadoVenta.EMITIDA
                || venta.getEstadoSunat() != EstadoSunat.PENDIENTE
                || venta.isValidadoSunat()
                || venta.getTipoComprobante() == TipoComprobante.NOTA_PEDIDO) {

            return;
        }

        boolean factura =
                venta.getTipoComprobante()
                        == TipoComprobante.FACTURA;

        String serieEsperada = factura ? "F001" : "B001";

        if (!serieEsperada.equals(venta.getSerie())) {
            return;
        }

        if (venta.getDetalles() == null
                || venta.getDetalles().isEmpty()
                || venta.getDetalles().size() > 50) {

            throw new IllegalArgumentException(
                    "SUNAT Beta admite entre 1 y 50 detalles"
            );
        }

        // Datos exclusivamente ficticios para SUNAT Beta.
        String rucEmisor = "20123456786";

        String razonSocialEmisor =
                "MARANATHA BETA - PRUEBA";

        String numeroDocumento = "20123456786";

        String nombreCliente =
                "EMPRESA FICTICIA DE PRUEBA";

        String tipoComprobante = factura ? "01" : "03";

        List<FacturacionClient.Detalle> detalles =
                new ArrayList<>();

        int posicion = 1;

        for (var detalle : venta.getDetalles()) {

            if (detalle.getCantidad() == null
                    || detalle.getCantidad() <= 0
                    || detalle.getPrecioUnitario() == null
                    || detalle.getPrecioUnitario().signum() <= 0
                    || detalle.getAfectacionIgv() == null) {

                throw new IllegalArgumentException(
                        "Detalle no valido para SUNAT Beta"
                );
            }

            detalles.add(
                    new FacturacionClient.Detalle(
                            "Producto de prueba " + posicion,
                            detalle.getCantidad(),
                            detalle.getPrecioUnitario(),
                            detalle.getAfectacionIgv()
                    )
            );

            posicion++;
        }

        var fechaEmision = venta.getFecha().toLocalDate();

        // Reserva persistente del correlativo.
        VentaFiscalService.ReservaBeta reserva =
                ventaFiscalService.reservarComprobanteBeta(
                        ventaId
                );

        if (reserva == null) {
            return;
        }

        FacturacionClient.SolicitudEmision solicitud =
                new FacturacionClient.SolicitudEmision(
                        rucEmisor,
                        razonSocialEmisor,
                        numeroDocumento,
                        nombreCliente,
                        reserva.serie(),
                        reserva.numero(),
                        detalles,
                        tipoComprobante,
                        "6",
                        null,
                        fechaEmision
                );

        FacturacionClient.ResultadoEmision resultado;

        try {

            resultado = facturacionClient.emitirBeta(
                    solicitud
            );

        } catch (Exception ex) {

            ventaFiscalService.marcarErrorComunicacion(
                    ventaId
            );

            log.warn(
                    "Emision Beta no confirmada para venta {}. Error: {}",
                    ventaId,
                    ex.getClass().getSimpleName()
            );

            return;
        }

        // Verificar que la respuesta corresponda al comprobante enviado.
        if (resultado == null
                || !"BETA".equals(resultado.ambiente())
                || !reserva.serie().equals(resultado.serie())
                || !reserva.numero().equals(resultado.numero())) {

            ventaFiscalService.marcarErrorComunicacion(
                    ventaId
            );

            return;
        }

        ventaFiscalService.registrarResultadoBeta(
                ventaId,
                resultado.estado(),
                resultado.codigo(),
                resultado.mensaje()
        );

        log.info(
                "Resultado SUNAT Beta para venta {}: {}-{} / {}",
                ventaId,
                reserva.serie(),
                reserva.numero(),
                resultado.estado()
        );
    }

    // Firma local original.
    @Transactional
    public void firmarVenta(Long ventaId) {

        if (betaHabilitado || !habilitado || ventaId == null) {
            return;
        }

        Venta venta = ventaRepository.findLockedById(ventaId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Venta no encontrada"
                        )
                );

        if (venta.getEstado() != EstadoVenta.EMITIDA
                || venta.getEstadoSunat() != EstadoSunat.PENDIENTE
                || venta.isValidadoSunat()
                || venta.getTipoComprobante() == TipoComprobante.NOTA_PEDIDO) {

            return;
        }

        boolean factura =
                venta.getTipoComprobante()
                        == TipoComprobante.FACTURA;

        String serieEsperada = factura ? "F001" : "B001";

        if (!serieEsperada.equals(venta.getSerie())) {
            return;
        }

        if (firmaLocalService.existe(ventaId)) {
            return;
        }

        // IDENTIDAD EXCLUSIVA PARA PRUEBAS LOCALES.
        // No representa un emisor comercial autorizado.

        String rucEmisor = "20123456786";

        String razonSocialEmisor =
                "MARANATHA STORE - PRUEBA SIN VALIDEZ FISCAL";

        String tipoComprobante = factura ? "01" : "03";

        // Receptor de laboratorio para evitar incorporar
        // documentos personales reales al XML de prueba.

        String tipoDocumentoCliente = factura ? "6" : "1";

        String numeroDocumento = factura
                ? "20123456786"
                : "00000000";

        String nombreCliente =
                "CLIENTE DE PRUEBA";

        List<FacturacionClient.Detalle> detalles =
                venta.getDetalles()
                        .stream()
                        .map(detalle ->
                                new FacturacionClient.Detalle(
                                        detalle.getProductoNombre(),
                                        detalle.getCantidad(),
                                        detalle.getPrecioUnitario(),
                                        detalle.getAfectacionIgv()
                                )
                        )
                        .toList();

        FacturacionClient.SolicitudEmision solicitud =
                new FacturacionClient.SolicitudEmision(
                        rucEmisor,
                        razonSocialEmisor,
                        numeroDocumento,
                        nombreCliente,
                        venta.getSerie(),
                        venta.getNumero(),
                        detalles,
                        tipoComprobante,
                        tipoDocumentoCliente,
                        null,
                        venta.getFecha().toLocalDate()
                );

        FacturacionClient.ResultadoFirma firma =
                facturacionClient.firmarLocal(solicitud);

        firmaLocalService.guardar(
                venta.getId(),
                venta.getSerie(),
                venta.getNumero(),
                venta.getTotal(),
                venta.getIgv(),
                firma
        );

        log.info(
                "Firma local almacenada para venta {}",
                ventaId
        );
    }
}