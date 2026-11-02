package com.libreria.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

public record FacturaPruebaRequest(

        @NotBlank
        String rucEmisor,

        @NotBlank
        String razonSocialEmisor,

        @NotBlank
        String rucCliente,

        @NotBlank
        String razonSocialCliente,

        @NotBlank
        String serie,

        @NotBlank
        String numero,

        @NotEmpty
        List<@Valid Detalle> detalles

) {

    public record Detalle(

            @NotBlank
            String descripcion,

            @NotNull
            @Positive
            Integer cantidad,

            @NotNull
            @Positive
            BigDecimal precioUnitario

    ) {}
}