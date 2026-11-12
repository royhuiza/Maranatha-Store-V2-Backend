package com.libreria.producto.model;

import com.libreria.producto.dto.AjusteStockRequest.Tipo;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "ajustes_inventario", indexes = @Index(columnList = "producto_id"))
public class AjusteInventario {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "producto_id", nullable = false)
    private Long productoId;
    /** T47: llave foránea ajustes_inventario.producto_id → productos.id (solo lectura; se guarda con productoId). */
    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", insertable = false, updatable = false,
            foreignKey = @ForeignKey(name = "fk_ajustes_inventario_producto"))
    private Producto producto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Tipo tipo;
    @Column(nullable = false)
    private int cantidad;
    @Column(nullable = false)
    private int stockAnterior;
    @Column(nullable = false)
    private int stockNuevo;
    @Column(nullable = false, length = 250)
    private String motivo;
    @Column(nullable = false)
    private Instant fecha;

    protected AjusteInventario() {}

    public AjusteInventario(Long productoId, Tipo tipo, int cantidad, int stockAnterior, int stockNuevo, String motivo) {
        this.productoId = productoId;
        this.tipo = tipo;
        this.cantidad = cantidad;
        this.stockAnterior = stockAnterior;
        this.stockNuevo = stockNuevo;
        this.motivo = motivo;
        this.fecha = Instant.now();
    }

    public Long getId() { return id; }
    public Tipo getTipo() { return tipo; }
    public int getCantidad() { return cantidad; }
    public int getStockAnterior() { return stockAnterior; }
    public int getStockNuevo() { return stockNuevo; }
    public String getMotivo() { return motivo; }
    public Instant getFecha() { return fecha; }
}
