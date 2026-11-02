
package com.libreria.facturacion.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.LocalDate;
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
        List<@Valid Detalle> detalles,

        @Pattern(regexp = "01|03")
        String tipoComprobante,

        String tipoDocumentoCliente,

        String direccionEmisor,

        LocalDate fechaEmision

) {

        // Constructor compatible con las pruebas anteriores
        public FacturaPruebaRequest(
                String rucEmisor,
                String razonSocialEmisor,
                String rucCliente,
                String razonSocialCliente,
                String serie,
                String numero,
                List<Detalle> detalles
        ) {
                this(
                        rucEmisor,
                        razonSocialEmisor,
                        rucCliente,
                        razonSocialCliente,
                        serie,
                        numero,
                        detalles,
                        "01",
                        "6",
                        null,
                        LocalDate.now()
                );
        }

        public record Detalle(

                @NotBlank
                String descripcion,

                @NotNull
                @Positive
                Integer cantidad,

                @NotNull
                @Positive
                BigDecimal precioUnitario,

                @NotNull
                TipoAfectacionIgv afectacionIgv

        ) {}
}
