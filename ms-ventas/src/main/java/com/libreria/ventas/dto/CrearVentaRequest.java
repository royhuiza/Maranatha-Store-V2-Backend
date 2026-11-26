package com.libreria.ventas.dto;

import com.libreria.ventas.model.MetodoPago;
import com.libreria.ventas.model.TipoComprobante;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

public class CrearVentaRequest {

    private MetodoPago metodoPago;

    private String compradorNombre;

    @Pattern(
            regexp = "^(?:\\d{8}|\\d{11})?$",
            message = "El documento debe tener 8 (DNI) u 11 (RUC) digitos"
    )
    private String documento;

    private Long clienteId;

    @Size(max = 10)
    private String tipoDocumento;

    @Size(max = 20)
    private String numeroDocumento;

    @Size(max = 220)
    private String clienteNombre;

    @NotNull(message = "El tipo de comprobante es obligatorio")
    private TipoComprobante tipoComprobante = TipoComprobante.BOLETA;

    @Valid
    @NotEmpty(message = "La venta debe contener al menos un producto")
    private List<DetalleVentaRequest> detalles = new ArrayList<>();

    @NotBlank(message = "La clave de idempotencia es obligatoria")
    @Size(max = 100)
    private String idempotencyKey;

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(MetodoPago metodoPago) {
        this.metodoPago = metodoPago;
    }

    public String getCompradorNombre() {
        return compradorNombre;
    }

    public void setCompradorNombre(String compradorNombre) {
        this.compradorNombre = compradorNombre;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public Long getClienteId() {
        return clienteId;
    }

    public void setClienteId(Long clienteId) {
        this.clienteId = clienteId;
    }

    public String getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getClienteNombre() {
        return clienteNombre;
    }

    public void setClienteNombre(String clienteNombre) {
        this.clienteNombre = clienteNombre;
    }

    public TipoComprobante getTipoComprobante() {
        return tipoComprobante;
    }

    public void setTipoComprobante(TipoComprobante tipoComprobante) {
        this.tipoComprobante = tipoComprobante;
    }

    public List<DetalleVentaRequest> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVentaRequest> detalles) {
        this.detalles = detalles;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }
}