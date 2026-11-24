package com.libreria.ventas.dto;

import com.libreria.ventas.model.Venta;

import java.math.BigDecimal;
import java.util.List;

/**
 * T67/T69: una página del historial de ventas.
 * totalElementos = ventas que cumplen el filtro; totalVentas = todas las registradas;
 * totalMonto = suma de las ventas filtradas que no están anuladas.
 */
public record HistorialVentas(List<Venta> contenido, int pagina, int tamano, long totalElementos,
                              int totalPaginas, BigDecimal totalMonto, long totalVentas) {}
