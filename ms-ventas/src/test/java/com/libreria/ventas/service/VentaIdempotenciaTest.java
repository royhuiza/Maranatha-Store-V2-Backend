package com.libreria.ventas.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.ventas.client.ClienteClient;
import com.libreria.ventas.client.ProductoClient;
import com.libreria.ventas.dto.CrearVentaRequest;
import com.libreria.ventas.dto.DetalleVentaRequest;
import com.libreria.ventas.dto.ProductoDTO;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class VentaIdempotenciaTest {

    private VentaRepository repository;
    private ClienteClient clienteClient;
    private ProductoClient productoClient;
    private VentaOperacionService operacionService;
    private VentaPersistenciaService persistenciaService;
    private VentaServiceImpl servicio;

    @BeforeEach
    void configurar() {

        repository = mock(VentaRepository.class);
        clienteClient = mock(ClienteClient.class);
        productoClient = mock(ProductoClient.class);

        operacionService =
                mock(VentaOperacionService.class);

        persistenciaService =
                mock(VentaPersistenciaService.class);

        servicio = new VentaServiceImpl(
                repository,
                clienteClient,
                productoClient,
                new ObjectMapper(),
                operacionService,
                new VentaRequestHashService(),
                persistenciaService
        );
    }

    private CrearVentaRequest crearSolicitud() {

        CrearVentaRequest request =
                new CrearVentaRequest();

        request.setIdempotencyKey(
                "VENTA-TEST-001");

        request.setTipoComprobante(
                TipoComprobante.BOLETA);

        DetalleVentaRequest detalle =
                new DetalleVentaRequest();

        detalle.setProductoId(10L);
        detalle.setCantidad(2);

        request.setDetalles(List.of(detalle));

        return request;
    }

    private VentaOperacion crearOperacion(
            EstadoVentaOperacion estado
    ) {

        VentaOperacion operacion =
                new VentaOperacion();

        ReflectionTestUtils.setField(operacion, "id", 1L);
        operacion.setEstado(estado);
        operacion.setIdempotencyKey(
                "VENTA-TEST-001");

        return operacion;
    }

    @Test
    void debeDevolverVentaExistenteSinDescontarStock() {

        CrearVentaRequest request = crearSolicitud();

        VentaOperacion operacion = crearOperacion(
                EstadoVentaOperacion.COMPLETADA
        );

        operacion.setVentaId(25L);

        Venta existente = new Venta();
        existente.setId(25L);

        when(operacionService.reservarConResultado(
                eq("VENTA-TEST-001"),
                anyString()
        )).thenReturn(
                new ResultadoReservaVenta(
                        operacion, false)
        );

        when(repository.findOneById(25L))
                .thenReturn(Optional.of(existente));

        Venta resultado =
                servicio.crearVenta(request);

        assertEquals(25L, resultado.getId());

        verifyNoInteractions(productoClient);
        verifyNoInteractions(persistenciaService);

        System.out.println(
                "OK: VENTA DUPLICADA BLOQUEADA");
    }

    @Test
    void debeBloquearOperacionEnProceso() {

        VentaOperacion operacion = crearOperacion(
                EstadoVentaOperacion.EN_PROCESO
        );

        when(operacionService.reservarConResultado(
                anyString(),
                anyString()
        )).thenReturn(
                new ResultadoReservaVenta(
                        operacion, false)
        );

        when(persistenciaService.buscarPorIdempotencyKey(
                "VENTA-TEST-001"
        )).thenReturn(Optional.empty());
        assertThrows(
                ResponseStatusException.class,
                () -> servicio.crearVenta(crearSolicitud())
        );

        verifyNoInteractions(productoClient);
        verify(persistenciaService, never())
                .guardarVenta(
                        any(Venta.class),
                        anyString()
                );

        System.out.println(
                "OK: OPERACION SIMULTANEA BLOQUEADA");
    }

    @Test
    void debeBloquearOperacionEnRevision() {

        VentaOperacion operacion = crearOperacion(
                EstadoVentaOperacion.REQUIERE_REVISION
        );

        when(operacionService.reservarConResultado(
                anyString(),
                anyString()
        )).thenReturn(
                new ResultadoReservaVenta(
                        operacion, false)
        );

        when(persistenciaService.buscarPorIdempotencyKey(
                "VENTA-TEST-001"
        )).thenReturn(Optional.empty());

        assertThrows(
                ResponseStatusException.class,
                () -> servicio.crearVenta(crearSolicitud())
        );

        verifyNoInteractions(productoClient);

        verify(persistenciaService, times(1))
                .buscarPorIdempotencyKey(
                        "VENTA-TEST-001"
                );

        verify(persistenciaService, never())
                .guardarVenta(
                        any(Venta.class),
                        anyString()
                );

        System.out.println(
                "OK: OPERACION EN REVISION BLOQUEADA");
    }


    @Test
    void debeRegistrarVentaNuevaUnaSolaVez() {

        VentaOperacion operacion = crearOperacion(
                EstadoVentaOperacion.EN_PROCESO
        );

        when(operacionService.reservarConResultado(
                anyString(),
                anyString()
        )).thenReturn(
                new ResultadoReservaVenta(
                        operacion, true)
        );

        ProductoDTO producto = new ProductoDTO();

        producto.setId(10L);
        producto.setNombre("Cuaderno de prueba");
        producto.setPrecio(new BigDecimal("5.00"));
        producto.setStock(100);
        producto.setActivo(true);
        producto.setAfectacionIgv("GRAVADO");

        when(productoClient.obtenerProducto(10L))
                .thenReturn(producto);

        when(persistenciaService.guardarVenta(
                any(Venta.class),
                eq("VENTA-TEST-001")
        )).thenAnswer(invocacion -> {

            Venta venta = invocacion.getArgument(0);

            venta.setId(25L);
            venta.setNumero("00000025");

            return venta;
        });

        Venta resultado =
                servicio.crearVenta(crearSolicitud());

        assertEquals(25L, resultado.getId());

        assertEquals(
                new BigDecimal("10.00"),
                resultado.getTotal()
        );

        verify(productoClient, times(1))
                .descontarStockIdempotente(
                        10L,
                        2,
                        "VENTA-1-10"
                );

        verify(persistenciaService, times(1))
                .guardarVenta(
                        any(Venta.class),
                        eq("VENTA-TEST-001")
                );

        verify(operacionService, times(1))
                .completar(1L, 25L);

        System.out.println(
                "OK: VENTA NUEVA REGISTRADA UNA SOLA VEZ");
    }

    @Test
    void debeDetectarCambioEnLaSolicitud() {

        VentaRequestHashService hashService =
                new VentaRequestHashService();

        CrearVentaRequest primera =
                crearSolicitud();

        CrearVentaRequest segunda =
                crearSolicitud();

        segunda.getDetalles()
                .get(0).setCantidad(5);

        assertNotEquals(
                hashService.calcular(primera),
                hashService.calcular(segunda)
        );

        System.out.println(
                "OK: CAMBIO DE SOLICITUD DETECTADO");
    }

    @Test
    void debeRecuperarVentaGuardadaTrasInterrupcion() {

        VentaOperacion operacion = crearOperacion(
                EstadoVentaOperacion.EN_PROCESO
        );

        Venta guardada = new Venta();

        guardada.setId(25L);
        guardada.setNumero("00000025");

        when(operacionService.reservarConResultado(
                anyString(),
                anyString()
        )).thenReturn(
                new ResultadoReservaVenta(
                        operacion, false)
        );

        when(persistenciaService.buscarPorIdempotencyKey(
                "VENTA-TEST-001"
        )).thenReturn(Optional.of(guardada));

        when(repository.findOneById(25L))
                .thenReturn(Optional.of(guardada));

        Venta resultado =
                servicio.crearVenta(crearSolicitud());

        assertEquals(25L, resultado.getId());

        verifyNoInteractions(productoClient);

        verify(persistenciaService, never())
                .guardarVenta(
                        any(Venta.class),
                        anyString()
                );

        verify(operacionService)
                .completar(1L, 25L);

        System.out.println(
                "OK: VENTA INTERRUMPIDA RECUPERADA");
    }

    @Test
    void debeRecuperarVentaGuardadaEnRevision() {

        VentaOperacion operacion = crearOperacion(
                EstadoVentaOperacion.REQUIERE_REVISION
        );

        Venta guardada = new Venta();

        guardada.setId(30L);
        guardada.setNumero("00000030");

        when(operacionService.reservarConResultado(
                anyString(),
                anyString()
        )).thenReturn(
                new ResultadoReservaVenta(
                        operacion, false)
        );

        when(persistenciaService.buscarPorIdempotencyKey(
                "VENTA-TEST-001"
        )).thenReturn(Optional.of(guardada));

        when(repository.findOneById(30L))
                .thenReturn(Optional.of(guardada));

        Venta resultado =
                servicio.crearVenta(crearSolicitud());

        assertEquals(30L, resultado.getId());

        verifyNoInteractions(productoClient);

        verify(persistenciaService, never())
                .guardarVenta(
                        any(Venta.class),
                        anyString()
                );

        verify(operacionService)
                .completar(1L, 30L);

        System.out.println(
                "OK: VENTA EN REVISION RECUPERADA");
    }

}
