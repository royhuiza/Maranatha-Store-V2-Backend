
package com.libreria.facturacion.controller;

import com.libreria.facturacion.dto.ConsultaRucResponse;
import com.libreria.facturacion.service.ConsultaRucService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class ConsultaRucControllerTest {

    private ConsultaRucController controller;

    @BeforeEach
    void configurar() {
        ConsultaRucService servicio = ruc -> {
            if (ruc == null || !ruc.matches("\\d{11}")) {
                throw new IllegalArgumentException(
                        "El RUC debe contener exactamente 11 digitos"
                );
            }

            throw new IllegalStateException(
                    "La consulta RUC no esta configurada"
            );
        };

        controller = new ConsultaRucController(servicio);
    }

    @Test
    void debeRechazarRucInvalido() {

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> controller.consultar("12345678")
        );

        ResponseEntity<String> respuesta =
                controller.manejarRucInvalido(error);

        assertEquals(
                HttpStatus.BAD_REQUEST,
                respuesta.getStatusCode()
        );
    }

    @Test
    void debeResponderServicioNoDisponible() {

        IllegalStateException error = assertThrows(
                IllegalStateException.class,
                () -> controller.consultar("20123456786")
        );

        ResponseEntity<String> respuesta =
                controller.manejarServicioNoDisponible(error);

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                respuesta.getStatusCode()
        );
    }

    @Test
    void debeDevolverDatosCuandoElServicioResponde() {

        ConsultaRucResponse datos = new ConsultaRucResponse(
                "20123456786",
                "EMPRESA DE PRUEBA",
                "ACTIVO",
                "HABIDO",
                "LIMA"
        );

        ConsultaRucService servicioSimulado = ruc -> datos;

        ConsultaRucController controlador =
                new ConsultaRucController(servicioSimulado);

        ResponseEntity<ConsultaRucResponse> respuesta =
                controlador.consultar("20123456786");

        assertEquals(
                HttpStatus.OK,
                respuesta.getStatusCode()
        );

        assertNotNull(respuesta.getBody());

        assertEquals(
                "EMPRESA DE PRUEBA",
                respuesta.getBody().razonSocial()
        );
    }
}
