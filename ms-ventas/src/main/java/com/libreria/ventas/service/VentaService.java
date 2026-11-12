package com.libreria.ventas.service;

import com.libreria.ventas.dto.CrearVentaRequest;
import com.libreria.ventas.model.Venta;

import java.util.List;

public interface VentaService {
    List<Venta> listarVentas();
    Venta obtenerVenta(Long id);
    Venta crearVenta(CrearVentaRequest request);
    boolean tieneVentas(Long productoId);
    Venta anularVenta(Long id);
    void eliminarVenta(Long id);
}
