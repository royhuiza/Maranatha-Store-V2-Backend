package com.libreria.ventas.dto;

import com.libreria.ventas.model.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.ArrayList;
import java.util.List;

public class CrearVentaRequest {
    private Long clienteId;
    @NotNull(message = "El tipo de comprobante es obligatorio")
    private TipoComprobante tipoComprobante = TipoComprobante.BOLETA;
    @Valid
    @NotEmpty(message = "La venta debe contener al menos un producto")
    private List<DetalleVentaRequest> detalles = new ArrayList<>();

    public Long getClienteId() { return clienteId; }
    public void setClienteId(Long clienteId) { this.clienteId = clienteId; }
    public TipoComprobante getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(TipoComprobante tipoComprobante) { this.tipoComprobante = tipoComprobante; }
    public List<DetalleVentaRequest> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleVentaRequest> detalles) { this.detalles = detalles; }
}
