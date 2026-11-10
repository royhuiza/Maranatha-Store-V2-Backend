package com.libreria.producto.service;

import com.libreria.producto.model.Producto;
import com.libreria.producto.repository.ProductoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** T06: bloqueo de artículos con nombre repetido. */
class NombreDuplicadoTest {

    @Test
    void crearConNombreRepetidoSeRechaza() {
        var repo = mock(ProductoRepository.class);
        when(repo.existsByNombreIgnoreCase("Cuaderno A4")).thenReturn(true);
        var ex = assertThrows(ResponseStatusException.class,
                () -> new ProductoServiceImpl(repo, null, null, null).crearProducto(producto("  Cuaderno A4 ")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        verify(repo, never()).save(any());
    }

    @Test
    void crearConNombreNuevoSeGuardaSinEspaciosExtra() {
        var repo = mock(ProductoRepository.class);
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        var guardado = new ProductoServiceImpl(repo, null, null, null).crearProducto(producto("  Cuaderno    A5 "));
        assertEquals("Cuaderno A5", guardado.getNombre());
    }

    @Test
    void editarSinCambiarNombreNoValidaDuplicado() {
        var repo = mock(ProductoRepository.class);
        var actual = producto("Cuaderno prueba 01");
        when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(actual));
        when(repo.save(any())).thenAnswer(i -> i.getArgument(0));
        new ProductoServiceImpl(repo, null, null, null).actualizarProducto(1L, producto("Cuaderno prueba 01"));
        verify(repo, never()).existsByNombreIgnoreCaseAndIdNot(any(), any());
    }

    @Test
    void renombrarAUnNombreExistenteSeRechaza() {
        var repo = mock(ProductoRepository.class);
        when(repo.findByIdForUpdate(1L)).thenReturn(Optional.of(producto("Lápiz HB")));
        when(repo.existsByNombreIgnoreCaseAndIdNot("Lapicero azul", 1L)).thenReturn(true);
        assertThrows(ResponseStatusException.class,
                () -> new ProductoServiceImpl(repo, null, null, null).actualizarProducto(1L, producto("Lapicero azul")));
        verify(repo, never()).save(any());
    }

    private Producto producto(String nombre) {
        var p = new Producto();
        p.setNombre(nombre);
        p.setDescripcion("Prueba");
        p.setPrecio(new BigDecimal("2.50"));
        p.setStock(10);
        p.setActivo(true);
        return p;
    }
}
