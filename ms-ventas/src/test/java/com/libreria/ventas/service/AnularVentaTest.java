package com.libreria.ventas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.ventas.client.ProductoClient;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.Venta;
import com.libreria.ventas.model.VentaDetalle;
import com.libreria.ventas.repository.VentaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * La venta anulada debe poder serializarse (el detalle viene cargado por el
 * repositorio). Antes, sin EntityGraph en findLockedById, la coleccion
 * "detalles" quedaba lazy y Jackson fallaba con LazyInitializationException.
 */
class AnularVentaTest {

    @Test
    void anulaVentaYDevuelveElDetalleCargado() {
        var repo = mock(VentaRepository.class);
        var productos = mock(ProductoClient.class);
        var venta = ventaConDetalle();

        when(repo.findLockedById(1L)).thenReturn(Optional.of(venta));
        when(repo.save(any(Venta.class))).thenAnswer(i -> i.getArgument(0));

        var resultado = new VentaServiceImpl(repo, null, productos, new ObjectMapper(), null)
                .anularVenta(1L);

        assertEquals(EstadoVenta.ANULADA, resultado.getEstado());
        assertEquals(1, resultado.getDetalles().size());
        assertEquals(2, resultado.getDetalles().get(0).getCantidad());
        verify(productos).reponerStock(10L, 2);
    }

    @Test
    void noReponeStockSiLaVentaYaEstaAnulada() {
        var repo = mock(VentaRepository.class);
        var productos = mock(ProductoClient.class);
        var venta = ventaConDetalle();
        venta.setEstado(EstadoVenta.ANULADA);

        when(repo.findLockedById(1L)).thenReturn(Optional.of(venta));

        var resultado = new VentaServiceImpl(repo, null, productos, new ObjectMapper(), null)
                .anularVenta(1L);

        assertEquals(EstadoVenta.ANULADA, resultado.getEstado());
        verify(productos, never()).reponerStock(anyLong(), anyInt());
    }

    private Venta ventaConDetalle() {
        var venta = new Venta();
        venta.setId(1L);
        venta.setEstado(EstadoVenta.EMITIDA);
        venta.setEstadoPago("PENDIENTE");

        var detalle = new VentaDetalle();
        detalle.setProductoId(10L);
        detalle.setCantidad(2);
        venta.agregarDetalle(detalle);
        return venta;
    }
}
