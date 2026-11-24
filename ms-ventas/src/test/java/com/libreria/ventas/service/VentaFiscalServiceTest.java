package com.libreria.ventas.service;

import com.libreria.ventas.model.EstadoSunat;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.TipoComprobante;
import com.libreria.ventas.repository.VentaRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VentaFiscalServiceTest {

    private VentaRepository repository;
    private VentaFiscalService servicio;

    @BeforeEach
    void configurar() {

        repository = mock(VentaRepository.class);

        servicio = new VentaFiscalService(repository);
    }

    @Test
    void debeReservarEmisionPendiente() {

        when(repository.reservarEmisionFiscal(
                1L,
                EstadoSunat.PENDIENTE,
                EstadoSunat.EN_PROCESO,
                EstadoVenta.EMITIDA,
                TipoComprobante.NOTA_PEDIDO
        )).thenReturn(1);

        assertTrue(servicio.iniciarEmision(1L));

        System.out.println("OK: RESERVA FISCAL");
    }

    @Test
    void debeBloquearSegundaEmision() {

        when(repository.reservarEmisionFiscal(
                1L,
                EstadoSunat.PENDIENTE,
                EstadoSunat.EN_PROCESO,
                EstadoVenta.EMITIDA,
                TipoComprobante.NOTA_PEDIDO
        )).thenReturn(1, 0);

        assertTrue(servicio.iniciarEmision(1L));
        assertFalse(servicio.iniciarEmision(1L));

        System.out.println("OK: CONTROL DE SEGUNDA RESERVA");
    }

    @Test
    void debeRegistrarAceptacionBeta() {

        when(repository.actualizarResultadoFiscal(
                eq(1L),
                eq(EstadoSunat.EN_PROCESO),
                eq(EstadoSunat.ACEPTADO_BETA),
                eq("0"),
                anyString(),
                any()
        )).thenReturn(1);

        assertDoesNotThrow(() ->
                servicio.registrarResultadoBeta(
                        1L,
                        "ACEPTADA_BETA",
                        "0",
                        "Factura aceptada"
                )
        );

        System.out.println("OK: ACEPTACION BETA");
    }

    @Test
    void debeRegistrarErrorComunicacion() {

        when(repository.actualizarResultadoFiscal(
                eq(1L),
                eq(EstadoSunat.EN_PROCESO),
                eq(EstadoSunat.ERROR_COMUNICACION),
                isNull(),
                anyString(),
                any()
        )).thenReturn(1);

        assertDoesNotThrow(() ->
                servicio.marcarErrorComunicacion(1L)
        );

        System.out.println("OK: ERROR DE COMUNICACION");
    }

    @Test
    void debeRegistrarRespuestaParaRevision() {

        when(repository.actualizarResultadoFiscal(
                eq(1L),
                eq(EstadoSunat.EN_PROCESO),
                eq(EstadoSunat.REQUIERE_REVISION),
                eq("3205"),
                anyString(),
                any()
        )).thenReturn(1);

        assertDoesNotThrow(() ->
                servicio.registrarResultadoBeta(
                        1L,
                        "ERROR_SOAP",
                        "3205",
                        "Respuesta requiere revision"
                )
        );

        System.out.println("OK: REVISION FISCAL");
    }

    @Test
    void debeImpedirActualizarEmisionInactiva() {

        assertThrows(
                ResponseStatusException.class,
                () -> servicio.registrarResultadoBeta(
                        1L,
                        "ACEPTADA_BETA",
                        "0",
                        "Factura aceptada"
                )
        );

        System.out.println("OK: EMISION INACTIVA BLOQUEADA");
    }
}
