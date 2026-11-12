
package com.libreria.facturacion.controller;

import com.libreria.facturacion.dto.ConsultaRucResponse;
import com.libreria.facturacion.service.ConsultaRucService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import com.libreria.facturacion.security.JwtService;

@WebMvcTest(ConsultaRucController.class)
@AutoConfigureMockMvc(addFilters = false)
class ConsultaRucControllerHttpTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ConsultaRucService consultaRucService;

    @MockBean
    private JwtService jwtService;

    @Test
    void debeResponder400CuandoRucEsInvalido() throws Exception {

        when(consultaRucService.consultarPorRuc("123"))
                .thenThrow(new IllegalArgumentException(
                        "El RUC debe contener exactamente 11 digitos"
                ));

        mockMvc.perform(get("/api/facturacion/ruc/123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(
                        "El RUC debe contener exactamente 11 digitos"
                ));
    }

    @Test
    void debeResponder503CuandoServicioNoEstaDisponible()
            throws Exception {

        when(consultaRucService.consultarPorRuc("20123456786"))
                .thenThrow(new IllegalStateException(
                        "La consulta RUC no esta configurada"
                ));

        mockMvc.perform(
                        get("/api/facturacion/ruc/20123456786")
                )
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().string(
                        "La consulta RUC no esta disponible"
                ));
    }

    @Test
    void debeResponder200CuandoConsultaEsExitosa()
            throws Exception {

        ConsultaRucResponse respuesta = new ConsultaRucResponse(
                "20123456786",
                "EMPRESA DE PRUEBA",
                "ACTIVO",
                "HABIDO",
                "LIMA"
        );

        when(consultaRucService.consultarPorRuc("20123456786"))
                .thenReturn(respuesta);

        mockMvc.perform(
                        get("/api/facturacion/ruc/20123456786")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ruc")
                        .value("20123456786"))
                .andExpect(jsonPath("$.razonSocial")
                        .value("EMPRESA DE PRUEBA"))
                .andExpect(jsonPath("$.estado")
                        .value("ACTIVO"))
                .andExpect(jsonPath("$.condicion")
                        .value("HABIDO"));
    }
}
