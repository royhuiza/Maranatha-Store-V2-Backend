package com.libreria.ventas.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
// T68: índices para filtrar el historial por fecha y por serie/número de comprobante.
@Table(name = "ventas", indexes = {
        @Index(name = "idx_ventas_fecha", columnList = "fecha"),
        @Index(name = "idx_ventas_serie_numero", columnList = "serie, numero")
})
public class Venta {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "cliente_id")
    private Long clienteId;

    @Column(name = "cliente_nombre", length = 220)
    private String clienteNombre;

    @Column(name = "cliente_documento", length = 40)
    private String clienteDocumento;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_comprobante", nullable = false, length = 30)
    private TipoComprobante tipoComprobante;

    @Column(nullable = false, length = 10)
    private String serie;

    @Column(nullable = false, length = 12)
    private String numero;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoVenta estado;

    @Column(name = "estado_pago", nullable = false, columnDefinition = "varchar(30) default 'PENDIENTE'")
    private String estadoPago = "PENDIENTE";
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }
    @Column(name = "validado_sunat", nullable = false, columnDefinition = "boolean default false")
    private boolean validadoSunat;
    public boolean isValidadoSunat() { return validadoSunat; }
    public void setValidadoSunat(boolean value) { validadoSunat = value; }
    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 30)
    private MetodoPago metodoPago;
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago value) { metodoPago = value; }

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_sunat", length = 30)
    private EstadoSunat estadoSunat;

    @Column(name = "idempotency_key", length = 100, unique = true)
    private String idempotencyKey;

    @Column(name = "codigo_respuesta_sunat", length = 30)
    private String codigoRespuestaSunat;

    @Column(name = "mensaje_respuesta_sunat", columnDefinition = "TEXT")
    private String mensajeRespuestaSunat;

    @Column(name = "fecha_respuesta_sunat")
    private LocalDateTime fechaRespuestaSunat;

    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<VentaDetalle> detalles = new ArrayList<>();

    public Venta() {}

    public void agregarDetalle(VentaDetalle detalle) {
        detalles.add(detalle);
        detalle.setVenta(this);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public String getClienteNombre() { return clienteNombre; }
    public void setClienteNombre(String clienteNombre) { this.clienteNombre = clienteNombre; }
    public String getClienteDocumento() { return clienteDocumento; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }
    // Preserve the original database timestamp; convert only when its origin is known.
    @Column(name="fecha_zona", length=40)
    private String fechaZona;
    public String getFechaZona() { return fechaZona; }
    public void setFechaZona(String zone) { fechaZona=zone; }
    public java.time.OffsetDateTime getFecha() { return PeruTime.fromStored(fecha,fechaZona); }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getBaseImponible() { return total == null ? BigDecimal.ZERO : total.subtract(getIgv()); }
    public BigDecimal getIgv() {
        if (total == null) return BigDecimal.ZERO;
        // Historical sales without classification retain the previous estimate.
        if (detalles.isEmpty() || detalles.stream().anyMatch(d -> d.getAfectacionIgv() == null)) {
            return total.subtract(total.divide(new BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP));
        }
        return detalles.stream().filter(d -> "GRAVADO".equals(d.getAfectacionIgv()))
                .map(d -> d.getSubtotal().subtract(d.getSubtotal().divide(new BigDecimal("1.18"), 2, java.math.RoundingMode.HALF_UP)))
                .reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
    }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public TipoComprobante getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(TipoComprobante tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public String getSerie() { return serie; }
    public void setSerie(String serie) { this.serie = serie; }
    public String getNumero() { return numero; }
    public void setNumero(String numero) { this.numero = numero; }
    public EstadoVenta getEstado() { return estado; }
    public void setEstado(EstadoVenta estado) { this.estado = estado; }
    public List<VentaDetalle> getDetalles() { return detalles; }
    public void setDetalles(List<VentaDetalle> detalles) {
        this.detalles.clear();
        if (detalles != null) detalles.forEach(this::agregarDetalle);
    }
    public EstadoSunat getEstadoSunat() {
        return estadoSunat;
    }

    public void setEstadoSunat(EstadoSunat estadoSunat) {
        this.estadoSunat = estadoSunat;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getCodigoRespuestaSunat() {
        return codigoRespuestaSunat;
    }

    public void setCodigoRespuestaSunat(String codigoRespuestaSunat) {
        this.codigoRespuestaSunat = codigoRespuestaSunat;
    }

    public String getMensajeRespuestaSunat() {
        return mensajeRespuestaSunat;
    }

    public void setMensajeRespuestaSunat(String mensajeRespuestaSunat) {
        this.mensajeRespuestaSunat = mensajeRespuestaSunat;
    }

    public LocalDateTime getFechaRespuestaSunat() {
        return fechaRespuestaSunat;
    }

    public void setFechaRespuestaSunat(LocalDateTime fechaRespuestaSunat) {
        this.fechaRespuestaSunat = fechaRespuestaSunat;
    }
}
