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
        // La ruta previa no puede recuperar un carrito diferente con la misma clave.
        if (venta.getDetalles().size()!=1 || !"Producto de prueba".equals(venta.getDetalles().get(0).getProductoNombre()))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"Clave asignada a otro carrito");
        return emision.emitirFacturaPrueba(venta.getId(), ruc);
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Venta registrarCatalogo(UUID operacion, int a, int b, int c) {
        if (!"db_ventas_test".equals(jdbc.queryForObject("SELECT current_database()", String.class)))
            throw new IllegalStateException("Solo db_ventas_test");
        return registrarLineas(operacion, CatalogoBeta.lineas(a,b,c));
    }
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public Venta registrarCatalogoCompleto(UUID operacion, String items, long centimos) {
        if (!"db_ventas_test".equals(jdbc.queryForObject("SELECT current_database()", String.class)))
            throw new IllegalStateException("Solo db_ventas_test");
        return registrarLineas(operacion,CatalogoBeta.lineas(items,centimos));
    }
    private Venta registrarLineas(UUID operacion, java.util.List<CatalogoBeta.Linea> lineas) {
        return registrarLineas(operacion,lineas,TipoComprobante.FACTURA);
    }
    /** Entrada del checkout normal, exclusivamente en la instancia de pruebas. */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public synchronized Venta confirmarDemo(com.libreria.ventas.dto.CrearVentaRequest request) {
        if (!"db_ventas_test".equals(jdbc.queryForObject("SELECT current_database()", String.class)))
            throw new IllegalStateException("Solo db_ventas_test");
        if (request.getTipoComprobante()!=TipoComprobante.FACTURA && request.getTipoComprobante()!=TipoComprobante.BOLETA)
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Esta fase admite factura y boleta de prueba");
        try {
            UUID id = UUID.fromString(request.getIdempotencyKey());
            if (!id.toString().equalsIgnoreCase(request.getIdempotencyKey())) throw new IllegalArgumentException();
            java.util.Map<Long,Integer> items = new java.util.LinkedHashMap<>();
            for (var d:request.getDetalles()) items.merge(d.getProductoId(),d.getCantidad(),Math::addExact);
            return registrarLineas(id,CatalogoBeta.lineasCheckout(items),request.getTipoComprobante());
        } catch (IllegalArgumentException | ArithmeticException ex) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"Carrito o clave de prueba invalida");
        }
    }
    private Venta registrarLineas(UUID operacion, java.util.List<CatalogoBeta.Linea> lineas, TipoComprobante tipo) {
        String clave = "DEMO-FISCAL-" + operacion;
        Venta v = persistencia.buscarPorIdempotencyKey(clave).orElse(null);
        if (v == null) {
            v = new Venta();
            v.setClienteNombre("EMPRESA FICTICIA DE PRUEBA");
            v.setClienteDocumento("RUC 20123456786");
            v.setFecha(LocalDateTime.now(ZoneId.of("America/Lima")));
            v.setFechaZona("America/Lima");
            v.setTipoComprobante(tipo);
            v.setSerie(tipo == TipoComprobante.BOLETA ? "B9B1" : "F9B1"); v.setNumero("PENDIENTE");
            v.setEstado(EstadoVenta.EMITIDA); v.setEstadoSunat(EstadoSunat.PENDIENTE);
            BigDecimal total = BigDecimal.ZERO;
            for (var x : lineas) {
                VentaDetalle det = new VentaDetalle();
                det.setProductoId(x.id()); det.setProductoNombre(x.nombre());
                det.setCantidad(x.cantidad()); det.setPrecioUnitario(x.precio());
                det.setAfectacionIgv(x.igv());
                BigDecimal sub = x.precio().multiply(BigDecimal.valueOf(x.cantidad()));
                det.setSubtotal(sub); total=total.add(sub); v.agregarDetalle(det);
            }
            v.setTotal(total); v = persistencia.guardarVenta(v, clave);
        }
        // Never reuse a UUID for a different cart (including legacy invoices).
        Venta guardada = v;
        if (v.getTipoComprobante()!=tipo || v.getDetalles().size()!=lineas.size() || !lineas.stream().allMatch(x -> vDetallesCoinciden(guardada, x)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT,"La clave corresponde a otro carrito de prueba");
        return emision.emitirFacturaPrueba(v.getId(), "20123456786");
    }
    private boolean vDetallesCoinciden(Venta v, CatalogoBeta.Linea x) {
        return v.getDetalles().stream().anyMatch(d -> x.nombre().equals(d.getProductoNombre()) &&
                java.util.Objects.equals(x.cantidad(),d.getCantidad()) && x.precio().compareTo(d.getPrecioUnitario())==0 && x.igv().equals(d.getAfectacionIgv()) && java.util.Objects.equals(x.id(),d.getProductoId()));
    }
}
