package com.libreria.ventas.dto;

public class InventarioOperacionDTO {

    private Long id;
    private String operationKey;
    private Long productoId;
    private Integer cantidad;
    private String tipo;
    private String operacionOrigenKey;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOperationKey() {
        return operationKey;
    }

    public void setOperationKey(String operationKey) {
        this.operationKey = operationKey;
    }

    public Long getProductoId() {
        return productoId;
    }

    public void setProductoId(Long productoId) {
        this.productoId = productoId;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getOperacionOrigenKey() {
        return operacionOrigenKey;
    }

    public void setOperacionOrigenKey(String operacionOrigenKey) {
        this.operacionOrigenKey = operacionOrigenKey;
    }
}