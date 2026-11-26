package com.libreria.ventas.service;

import com.libreria.ventas.client.FacturacionClient;
import com.libreria.ventas.client.FacturacionClient.Detalle;
import com.libreria.ventas.client.FacturacionClient.SolicitudEmision;
import com.libreria.ventas.client.FacturacionClient.ResultadoEmision;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

/** Coordinador exclusivo de laboratorio. No activar sobre db_ventas. */
@Service
@Profile({"test", "demo-fiscal"})
public class VentaEmisionBetaService {
    private final VentaRepository repository;
    private final VentaFiscalService fiscal;
    private final FacturacionClient client;
    private final JdbcTemplate jdbc;

    public VentaEmisionBetaService(VentaRepository repository,
            VentaFiscalService fiscal, FacturacionClient client, JdbcTemplate jdbc) {
        this.repository = repository;
        this.fiscal = fiscal;
        this.client = client;
        this.jdbc = jdbc;
    }

    public Venta emitirFacturaPrueba(Long id, String rucPrueba) {
        if (!"db_ventas_test".equals(jdbc.queryForObject(
                "SELECT current_database()", String.class))) {
            throw new IllegalStateException("Solo se permite db_ventas_test");
        }
        if (rucPrueba == null || !rucPrueba.matches("[0-9]{11}")) {
            throw new IllegalArgumentException("RUC de prueba requerido");
        }
        Venta venta = buscar(id);
        boolean boleta = venta.getTipoComprobante() == TipoComprobante.BOLETA;
        if ((!boleta && venta.getTipoComprobante() != TipoComprobante.FACTURA)
                || venta.getEstado() != EstadoVenta.EMITIDA
                || venta.getIdempotencyKey() == null
                || !venta.getIdempotencyKey().startsWith("DEMO-FISCAL-")
                || !(boleta ? "B9B1" : "F9B1").equals(venta.getSerie())
                || venta.getNumero() == null || !venta.getNumero().matches("[0-9]{1,8}")
                || !"EMPRESA FICTICIA DE PRUEBA".equals(venta.getClienteNombre())
                || !("RUC " + rucPrueba).equals(venta.getClienteDocumento())
                || venta.getDetalles().isEmpty() || venta.getDetalles().size() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Solo se admiten facturas sinteticas del laboratorio");
        }
        var lineas = venta.getDetalles();
        boolean legacy = !boleta && lineas.size()==1 && "Producto de prueba".equals(lineas.get(0).getProductoNombre())
                && java.util.Objects.equals(lineas.get(0).getCantidad(),1)
                && lineas.get(0).getPrecioUnitario()!=null && lineas.get(0).getPrecioUnitario().signum()>0
                && "GRAVADO".equals(lineas.get(0).getAfectacionIgv());
        boolean catalogo = lineas.stream().map(VentaDetalle::getProductoNombre).distinct().count()==lineas.size()
                && lineas.stream().allMatch(d -> CatalogoBeta.valida(d.getProductoNombre(),d.getPrecioUnitario(),d.getCantidad(),d.getAfectacionIgv()));
        if ((!legacy && !catalogo) || venta.getTotal()==null || venta.getTotal().compareTo(lineas.stream()
                .map(d -> d.getPrecioUnitario().multiply(java.math.BigDecimal.valueOf(d.getCantidad())))
                .reduce(java.math.BigDecimal.ZERO,java.math.BigDecimal::add))!=0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Detalle de laboratorio invalido");
        }
        SolicitudEmision solicitud = new SolicitudEmision(
                rucPrueba, "MARANATHA BETA - PRUEBA", rucPrueba,
                venta.getClienteNombre(), venta.getSerie(), venta.getNumero(),
                lineas.stream().map(d -> new Detalle(d.getProductoNombre(),d.getCantidad(),d.getPrecioUnitario(),d.getAfectacionIgv())).toList(),
                boleta ? "03" : "01", "6", null, venta.getFecha().toLocalDate());

        // La reserva se confirma en una transaccion independiente antes del HTTP.
        if (!fiscal.iniciarEmision(id)) return buscar(id);

        ResultadoEmision resultado;
        try {
            resultado = client.emitirBeta(solicitud);
        } catch (RuntimeException ex) {
            fiscal.marcarErrorComunicacion(id);
            return buscar(id);
        }
        // No asociar a la venta una respuesta de otro comprobante o ambiente.
        if (resultado == null || !"BETA".equals(resultado.ambiente())
                || !solicitud.serie().equals(resultado.serie())
                || !solicitud.numero().equals(resultado.numero())) {
            fiscal.registrarResultadoBeta(id, "RESPUESTA_INVALIDA", null,
                    "Respuesta no coincide con el comprobante; revisar antes de reenviar");
        } else {
            fiscal.registrarResultadoBeta(id, resultado.estado(), resultado.codigo(), resultado.mensaje());
        }
        return buscar(id);
    }

    @jakarta.persistence.PersistenceUnit
    private jakarta.persistence.EntityManagerFactory fiscalEmf;

    private Venta buscar(Long id) {
        var em = fiscalEmf.createEntityManager();
        try {
            return em.createQuery(
                "select distinct v from Venta v left join fetch v.detalles where v.id = :id",
                Venta.class
            )
            .setParameter("id", id)
            .setHint("jakarta.persistence.cache.retrieveMode",
                jakarta.persistence.CacheRetrieveMode.BYPASS)
            .getResultList()
            .stream()
            .findFirst()
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Venta no encontrada"));
        } finally {
            em.close();
        }
    }
}
