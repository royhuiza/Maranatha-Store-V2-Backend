package com.libreria.facturacion.controller;

import com.libreria.facturacion.dto.FacturaPruebaRequest;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/facturacion/emision")
public class EmisionInternaController {

    private final EmisionElectronicaController emisionController;

    public EmisionInternaController(
            EmisionElectronicaController emisionController
    ) {
        this.emisionController = emisionController;
    }

    @PostMapping("/beta")
    public EmisionElectronicaController.ResultadoEmision emitir(
            @Valid @RequestBody FacturaPruebaRequest factura
    ) {

        return emisionController.emitirBeta(factura);
    }
}
