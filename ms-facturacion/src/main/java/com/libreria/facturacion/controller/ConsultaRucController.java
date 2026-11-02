
package com.libreria.facturacion.controller;

import com.libreria.facturacion.dto.ConsultaRucResponse;
import com.libreria.facturacion.service.ConsultaRucService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facturacion/ruc")
public class ConsultaRucController {

    private final ConsultaRucService consultaRucService;

    public ConsultaRucController(
            ConsultaRucService consultaRucService
    ) {
        this.consultaRucService = consultaRucService;
    }

    @GetMapping("/{ruc}")
    public ResponseEntity<ConsultaRucResponse> consultar(
            @PathVariable String ruc
    ) {
        return ResponseEntity.ok(
                consultaRucService.consultarPorRuc(ruc)
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> manejarRucInvalido(
            IllegalArgumentException ex
    ) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler({
            IllegalStateException.class,
            UnsupportedOperationException.class
    })
    public ResponseEntity<String> manejarServicioNoDisponible(
            RuntimeException ex
    ) {
        return ResponseEntity.status(
                HttpStatus.SERVICE_UNAVAILABLE
        ).body("La consulta RUC no esta disponible");
    }
}
