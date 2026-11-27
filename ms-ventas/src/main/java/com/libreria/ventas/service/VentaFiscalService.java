package com.libreria.ventas.service;

import com.libreria.ventas.model.EstadoSunat;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.TipoComprobante;
import com.libreria.ventas.model.Venta;
import com.libreria.ventas.repository.VentaRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;

@Service
public class VentaFiscalService {

    private final VentaRepository ventaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public VentaFiscalService(
            VentaRepository ventaRepository
    ) {
        this.ventaRepository = ventaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean iniciarEmision(Long ventaId) {

        return ventaRepository.reservarEmisionFiscal(
                ventaId,
                EstadoSunat.PENDIENTE,
                EstadoSunat.EN_PROCESO,
                EstadoVenta.EMITIDA,
                TipoComprobante.NOTA_PEDIDO
        ) == 1;
    }

    // Reserva automatica del comprobante SUNAT Beta.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ReservaBeta reservarComprobanteBeta(Long ventaId) {

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

            return null;
        }

        String serie = switch (venta.getTipoComprobante()) {
            case FACTURA -> "F9B1";
            case BOLETA -> "B9B1";
            default -> throw new IllegalArgumentException(
                    "Tipo de comprobante no admitido"
            );
        };

        // Reservar la emision para evitar envios duplicados.
        boolean reservada = ventaRepository.reservarEmisionFiscal(
                ventaId,
                EstadoSunat.PENDIENTE,
                EstadoSunat.EN_PROCESO,
                EstadoVenta.EMITIDA,
                TipoComprobante.NOTA_PEDIDO
        ) == 1;

        if (!reservada) {
            return null;
        }

        // Obtener el siguiente correlativo persistente.
        Long correlativo = jdbcTemplate.queryForObject(
                "SELECT nextval('comprobante_beta_seq')",
                Long.class
        );

        if (correlativo == null
                || correlativo < 1
                || correlativo > 99999999L) {

            throw new IllegalStateException(
                    "Correlativo Beta no disponible"
            );
        }

        String numero = String.format(
                Locale.ROOT,
                "%08d",
                correlativo
        );

        // Vincular la venta interna con el comprobante Beta.
        jdbcTemplate.update(
                """
                INSERT INTO comprobante_beta
                    (venta_id, serie, numero)
                VALUES (?, ?, ?)
                """,
                ventaId,
                serie,
                numero
        );

        return new ReservaBeta(
                serie,
                numero
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarResultadoBeta(
            Long ventaId,
            String estado,
            String codigo,
            String mensaje
    ) {

        EstadoSunat nuevoEstado;

        if ("ACEPTADA_BETA".equals(estado)
                && "0".equals(codigo)) {

            nuevoEstado = EstadoSunat.ACEPTADO_BETA;

        } else {

            nuevoEstado = EstadoSunat.REQUIERE_REVISION;
        }

        actualizarResultado(
                ventaId,
                nuevoEstado,
                codigo,
                mensaje
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarErrorComunicacion(Long ventaId) {

        actualizarResultado(
                ventaId,
                EstadoSunat.ERROR_COMUNICACION,
                null,
                "No se pudo confirmar el resultado de SUNAT Beta. "
                        + "Verificar antes de reenviar."
        );
    }

    private void actualizarResultado(
            Long ventaId,
            EstadoSunat estado,
            String codigo,
            String mensaje
    ) {

        if (codigo != null && codigo.length() > 30) {
            codigo = codigo.substring(0, 30);
        }

        int actualizadas =
                ventaRepository.actualizarResultadoFiscal(
                        ventaId,
                        EstadoSunat.EN_PROCESO,
                        estado,
                        codigo,
                        mensaje,
                        LocalDateTime.now(
                                ZoneId.of("America/Lima"))
                );

        if (actualizadas != 1) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La venta no tiene una emision fiscal activa"
            );
        }
    }

    // Referencia del comprobante reservado en SUNAT Beta.
    public record ReservaBeta(
            String serie,
            String numero
    ) {}
}