package com.libreria.producto.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "operaciones_stock_venta")
public class OperacionStockVenta {
    @Id
    private UUID id;
    @Column(nullable = false, columnDefinition = "text")
    private String cantidades;
    @Column(nullable = false)
    private boolean revertida;

    protected OperacionStockVenta() {}
}
