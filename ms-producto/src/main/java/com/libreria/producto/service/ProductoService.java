package com.libreria.producto.service;

import com.libreria.producto.model.Producto;
import com.libreria.producto.model.TipoAfectacionIgv;

import java.util.List;

public interface ProductoService {

    List<Producto> listar();

    Producto obtenerProducto(Long id);

    Producto crearProducto(Producto producto);

    Producto actualizarProducto(Long id, Producto cambios);

    Producto actualizarAfectacionIgv(
            Long id,
            TipoAfectacionIgv afectacionIgv
    );

    Producto descontarStock(Long id, int cantidad);

    Producto reponerStock(Long id, int cantidad);

    void eliminarProducto(Long id);
}