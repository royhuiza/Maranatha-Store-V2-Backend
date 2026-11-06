package com.libreria.producto.service;

import com.libreria.producto.client.VentasClient;
import com.libreria.producto.model.Producto;
import com.libreria.producto.model.TipoAfectacionIgv;
import com.libreria.producto.repository.AjusteInventarioRepository;
import com.libreria.producto.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProductoServiceImplTest {

    private ProductoRepository repository;
    private ProductoServiceImpl service;

    @BeforeEach
    void configurar() {
        repository = mock(ProductoRepository.class);
        AjusteInventarioRepository ajustes = mock(AjusteInventarioRepository.class);
        VentasClient ventas = mock(VentasClient.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        service = new ProductoServiceImpl(repository, ajustes, ventas, jdbc);

        when(repository.save(any(Producto.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private Producto crearDatosProducto() {
        Producto producto = new Producto();
        producto.setNombre("Cuaderno A4");
        producto.setDescripcion("100 hojas");
        producto.setPrecio(new BigDecimal("13.00"));
        producto.setStock(28);
        producto.setActivo(true);
        return producto;
    }

    @Test
    void debeRegistrarProductoGravado() {
        Producto producto = crearDatosProducto();
        producto.setAfectacionIgv(TipoAfectacionIgv.GRAVADO);

        Producto resultado = service.crearProducto(producto);

        assertEquals(TipoAfectacionIgv.GRAVADO,
                resultado.getAfectacionIgv());
        assertEquals(new BigDecimal("13.00"),
                resultado.getPrecio());
        assertEquals(28, resultado.getStock());

        verify(repository).save(producto);
    }

    @Test
    void debeActualizarClasificacionSinCambiarPrecioNiStock() {
        Producto existente = crearDatosProducto();
        existente.setId(1L);
        existente.setAfectacionIgv(TipoAfectacionIgv.GRAVADO);

        when(repository.findById(1L))
                .thenReturn(Optional.of(existente));

        Producto cambios = crearDatosProducto();
        cambios.setAfectacionIgv(TipoAfectacionIgv.EXONERADO);

        Producto resultado = service.actualizarProducto(1L, cambios);

        assertEquals(TipoAfectacionIgv.EXONERADO,
                resultado.getAfectacionIgv());
        assertEquals(new BigDecimal("13.00"),
                resultado.getPrecio());
        assertEquals(28, resultado.getStock());

        verify(repository).save(existente);
    }

    @Test
    void debeConservarClasificacionSiNoSeEnviaOtra() {
        Producto existente = crearDatosProducto();
        existente.setId(2L);
        existente.setAfectacionIgv(TipoAfectacionIgv.INAFECTO);

        when(repository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(existente));

        Producto cambios = crearDatosProducto();

        Producto resultado = service.actualizarProducto(2L, cambios);

        assertEquals(TipoAfectacionIgv.INAFECTO,
                resultado.getAfectacionIgv());
    }

    @Test
    void debePermitirProductoSinClasificacion() {
        Producto producto = crearDatosProducto();

        Producto resultado = service.crearProducto(producto);

        assertNull(resultado.getAfectacionIgv());

        ArgumentCaptor<Producto> captor =
                ArgumentCaptor.forClass(Producto.class);

        verify(repository).save(captor.capture());

        assertNull(captor.getValue().getAfectacionIgv());
    }

    @Test
    void debeActualizarSoloLaClasificacionTributaria() {
        Producto existente = crearDatosProducto();
        existente.setId(1L);
        existente.setAfectacionIgv(TipoAfectacionIgv.GRAVADO);

        when(repository.findById(1L))
                .thenReturn(Optional.of(existente));

        Producto resultado = service.actualizarAfectacionIgv(
                1L,
                TipoAfectacionIgv.EXONERADO
        );

        assertEquals(TipoAfectacionIgv.EXONERADO,
                resultado.getAfectacionIgv());

        assertEquals(new BigDecimal("13.00"),
                resultado.getPrecio());

        assertEquals(28, resultado.getStock());
        assertEquals("Cuaderno A4", resultado.getNombre());

        verify(repository).save(existente);
    }

}
