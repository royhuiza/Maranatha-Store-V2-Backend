package com.libreria.producto.service;

import com.libreria.producto.model.Producto;
import com.libreria.producto.dto.AjusteStockRequest;
import com.libreria.producto.model.AjusteInventario;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ProductoService {
    List<Producto> listar(String consulta);
    Producto obtenerProducto(Long id);
    List<Producto> listarStockBajo();
    Producto crearProducto(Producto producto);
    Producto actualizarProducto(Long id, Producto producto);
    Producto descontarStock(Long id, int cantidad);
    Producto reponerStock(Long id, int cantidad);
    Producto ajustarStock(Long id, AjusteStockRequest ajuste);
    List<AjusteInventario> historialAjustes(Long id);
    void aplicarStockVenta(UUID idOperacion, Map<Long, Integer> cantidades, boolean reversa);
    void eliminarProducto(Long id);
}
