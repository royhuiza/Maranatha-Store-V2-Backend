package com.libreria.ventas.service;

import com.libreria.ventas.model.VentaOperacion;

public record ResultadoReservaVenta(
        VentaOperacion operacion,
        boolean nueva
) {
}