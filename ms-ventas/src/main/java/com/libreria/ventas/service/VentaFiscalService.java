package com.libreria.ventas.service;

import com.libreria.ventas.model.EstadoSunat;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.TipoComprobante;
import com.libreria.ventas.model.Venta;
import com.libreria.ventas.repository.VentaRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class VentaFiscalService {

    private final VentaRepository ventaRepository;

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
}
