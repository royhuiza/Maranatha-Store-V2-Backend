package com.libreria.producto.service;

import com.libreria.producto.model.Producto;
import com.libreria.producto.repository.ProductoRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** T39-T41: stock mínimo por artículo y estado calculado. */
class StockMinimoTest {

    @Test
    void estadoComparaStockConStockMinimo() {
        assertEquals("SIN_STOCK", producto(0, 5).getEstadoStock());
        assertEquals("STOCK_BAJO", producto(5, 5).getEstadoStock());
        assertEquals("DISPONIBLE", producto(6, 5).getEstadoStock());
        assertEquals("STOCK_BAJO", producto(10, 12).getEstadoStock());
        assertEquals("DISPONIBLE", producto(3, 2).getEstadoStock());
    }

    @Test
    void crearSinStockMinimoUsaCinco() {
        var repo = mock(ProductoRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var p = producto(10, 5);
        p.setStockMinimo(null);
        assertEquals(5, new ProductoServiceImpl(repo, null, null, null).crearProducto(p).getStockMinimo());
    }

    @Test
    void editarCambiaElStockMinimo() {
        var repo = mock(ProductoRepository.class);
        var actual = producto(10, 5);
        when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(actual));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var editado = new ProductoServiceImpl(repo, null, null, null).actualizarProducto(1L, producto(10, 15));
        assertEquals(15, editado.getStockMinimo());
        assertEquals("STOCK_BAJO", editado.getEstadoStock());
    }

    @Test
    void stockBajoDelegaEnLaConsultaDelRepositorio() {
        var repo = mock(ProductoRepository.class);
        var bajo = producto(2, 5);
        when(repo.findStockBajo()).thenReturn(List.of(bajo));
        assertEquals(List.of(bajo), new ProductoServiceImpl(repo, null, null, null).listarStockBajo());
    }

    private Producto producto(int stock, int minimo) {
        var p = new Producto();
        p.setNombre("Cuaderno");
        p.setDescripcion("A4");
        p.setPrecio(new BigDecimal("3.00"));
        p.setStock(stock);
        p.setStockMinimo(minimo);
        p.setActivo(true);
        return p;
    }
}
