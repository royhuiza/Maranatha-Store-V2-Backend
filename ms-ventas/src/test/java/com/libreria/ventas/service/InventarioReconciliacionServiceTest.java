package com.libreria.ventas.service;

import com.libreria.ventas.client.ProductoClient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InventarioReconciliacionServiceTest {

    private ProductoClient productoClient;

    private InventarioVerificacionService verificacion;

    private InventarioReconciliacionService servicio;

    @BeforeEach
    void configurar() {

        productoClient = mock(ProductoClient.class);

        verificacion =
                mock(InventarioVerificacionService.class);

        servicio = new InventarioReconciliacionService(
                productoClient,
                verificacion
        );
    }

    @Test
    void debeCompensarDescuentoConfirmado() {

        when(verificacion.descuentoConfirmado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(true);

        when(verificacion.descuentoCompensado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(false, true);

        assertTrue(servicio.compensarDescuentos(
                1L, Map.of(10L, 2)
        ));

        verify(productoClient, times(1))
                .reponerStockIdempotente(
                        10L,
                        2,
                        "COMPENSACION-1-10",
                        "VENTA-1-10"
                );

        System.out.println(
                "OK: DESCUENTO COMPENSADO");
    }

    @Test
    void debeEvitarReposicionDuplicada() {

        when(verificacion.descuentoConfirmado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(true);

        when(verificacion.descuentoCompensado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(true);

        assertTrue(servicio.compensarDescuentos(
                1L, Map.of(10L, 2)
        ));

        verifyNoInteractions(productoClient);

        System.out.println(
                "OK: REPOSICION DUPLICADA BLOQUEADA");
    }

    @Test
    void debeEvitarReponerDescuentoNoConfirmado() {

        when(verificacion.descuentoConfirmado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(false);

        assertFalse(servicio.compensarDescuentos(
                1L, Map.of(10L, 2)
        ));

        verifyNoInteractions(productoClient);

        System.out.println(
                "OK: DESCUENTO NO CONFIRMADO");
    }

    @Test
    void debeDetectarErrorDeComunicacion() {

        when(verificacion.descuentoConfirmado(
                10L, 2, "VENTA-1-10"
        )).thenThrow(
                new IllegalStateException(
                        "Servicio no disponible")
        );

        assertFalse(servicio.compensarDescuentos(
                1L, Map.of(10L, 2)
        ));

        verifyNoInteractions(productoClient);

        System.out.println(
                "OK: ERROR DE INVENTARIO CONTROLADO");
    }

    @Test
    void debeDetectarReposicionNoConfirmada() {

        when(verificacion.descuentoConfirmado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(true);

        when(verificacion.descuentoCompensado(
                10L, 2, "VENTA-1-10"
        )).thenReturn(false);

        assertFalse(servicio.compensarDescuentos(
                1L, Map.of(10L, 2)
        ));

        verify(productoClient, times(1))
                .reponerStockIdempotente(
                        10L,
                        2,
                        "COMPENSACION-1-10",
                        "VENTA-1-10"
                );

        System.out.println(
                "OK: REPOSICION PENDIENTE DE CONFIRMACION");
    }
}
