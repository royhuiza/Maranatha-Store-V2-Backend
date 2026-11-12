package com.libreria.producto.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.producto.model.Producto;
import com.libreria.producto.model.TipoAfectacionIgv;
import com.libreria.producto.service.ImportacionProductos;
import com.libreria.producto.service.ProductoService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ProductoControllerTest {

    private final ProductoService service = mock(ProductoService.class);
    private final ImportacionProductos importacion = mock(ImportacionProductos.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc crearMockMvc() {
        LocalValidatorFactoryBean validator =
                new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        return MockMvcBuilders
                .standaloneSetup(new ProductoController(service, importacion))
                .setValidator(validator)
                .build();
    }

    @Test
    void debeActualizarClasificacionTributaria() throws Exception {
        Producto producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Cuaderno A4");
        producto.setPrecio(new BigDecimal("13.00"));
        producto.setStock(28);
        producto.setAfectacionIgv(TipoAfectacionIgv.GRAVADO);

        when(service.actualizarAfectacionIgv(
                eq(1L), eq(TipoAfectacionIgv.GRAVADO)))
                .thenReturn(producto);

        crearMockMvc().perform(
                        patch("/productos/1/afectacion-igv")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {"afectacionIgv":"GRAVADO"}
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.afectacionIgv").value("GRAVADO"))
                .andExpect(jsonPath("$.precio").value(13.00))
                .andExpect(jsonPath("$.stock").value(28));

        verify(service).actualizarAfectacionIgv(
                1L, TipoAfectacionIgv.GRAVADO);
    }

    @Test
    void debeRechazarClasificacionVacia() throws Exception {
        crearMockMvc().perform(
                        patch("/productos/1/afectacion-igv")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}")
                )
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}
