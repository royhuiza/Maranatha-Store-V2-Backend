package com.libreria.producto.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class StockIdempotenteRequest {

    @Min(value = 1, message = "La cantidad debe ser mayor que cero")
    @Max(value = Integer.MAX_VALUE)
    private int cantidad;

    @NotBlank(message = "La clave de operación es obligatoria")
    @Size(max = 150, message = "La clave no puede superar 150 caracteres")
    private String operationKey;

    @Size(max = 150)
    private String operacionOrigenKey;

    public String getOperacionOrigenKey() {
        return operacionOrigenKey;
    }

    public void setOperacionOrigenKey(String operacionOrigenKey) {
        this.operacionOrigenKey = operacionOrigenKey;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public String getOperationKey() {
        return operationKey;
    }

    public void setOperationKey(String operationKey) {
        this.operationKey = operationKey;
    }
}