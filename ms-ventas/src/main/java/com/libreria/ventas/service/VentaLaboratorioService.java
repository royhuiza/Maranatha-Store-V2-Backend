package com.libreria.ventas.service;

import com.libreria.ventas.model.*;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Registro sintetico y emision coordinada, exclusivo de laboratorio. */
@Service
@Profile({"test", "demo-fiscal"})
public class VentaLaboratorioService {
    private final VentaPersistenciaService persistencia;
    private final VentaEmisionBetaService emision;
    private final JdbcTemplate jdbc;

    public VentaLaboratorioService(VentaPersistenciaService persistencia,
            VentaEmisionBetaService emision, JdbcTemplate jdbc) {
        this.persistencia = persistencia;
        this.emision = emision;
        this.jdbc = jdbc;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Venta registrarYEmitir(UUID operacion, String ruc) {
        if (!"db_ventas_test".equals(jdbc.queryForObject("SELECT current_database()", String.class))) {
            throw new IllegalStateException("Solo se permite db_ventas_test");
        }
        if (operacion == null || ruc == null || !ruc.matches("[0-9]{11}")) {
            throw new IllegalArgumentException("Operacion y RUC de laboratorio requeridos");
        }
        String clave = "DEMO-FISCAL-" + operacion;
        Venta venta = persistencia.buscarPorIdempotencyKey(clave).orElse(null);
        if (venta == null) {
            venta = new Venta();
            venta.setClienteNombre("EMPRESA FICTICIA DE PRUEBA");
            venta.setClienteDocumento("RUC " + ruc);
            venta.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
            venta.setFechaZona("America/Lima");
            venta.setTipoComprobante(TipoComprobante.FACTURA);
            venta.setSerie("F9B1");
            venta.setNumero("PENDIENTE");
            venta.setEstado(EstadoVenta.EMITIDA);
            venta.setEstadoSunat(EstadoSunat.PENDIENTE);
            venta.setTotal(new BigDecimal("11.80"));
            VentaDetalle detalle = new VentaDetalle();
            detalle.setProductoId(1L);
            detalle.setProductoNombre("Producto de prueba");
            detalle.setCantidad(1);
            detalle.setPrecioUnitario(new BigDecimal("11.80"));
            detalle.setSubtotal(new BigDecimal("11.80"));
            detalle.setAfectacionIgv("GRAVADO");
            venta.agregarDetalle(detalle);
            venta = persistencia.guardarVenta(venta, clave);
        }
        return emision.emitirFacturaPrueba(venta.getId(), ruc);
    }
}
