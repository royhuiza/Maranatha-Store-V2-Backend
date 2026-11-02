
package com.libreria.facturacion.dto;

public record ConsultaRucResponse(

        String ruc,

        String razonSocial,

        String estado,

        String condicion,

        String direccionFiscal

) {
}
