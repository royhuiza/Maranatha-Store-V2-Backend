
package com.libreria.facturacion.controller;

import com.libreria.facturacion.dto.FacturaPruebaRequest;
import com.libreria.facturacion.service.ComprobanteFirmaLocalService;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/facturacion/emision")
public class EmisionInternaController {

    private final EmisionElectronicaController emisionController;
    private final ComprobanteFirmaLocalService firmaLocalService;

    public EmisionInternaController(
            EmisionElectronicaController emisionController,
            ComprobanteFirmaLocalService firmaLocalService
    ) {
        this.emisionController = emisionController;
        this.firmaLocalService = firmaLocalService;
    }

    @PostMapping("/beta")
    public EmisionElectronicaController.ResultadoEmision emitir(
            @Valid @RequestBody FacturaPruebaRequest factura
    ) {
        return emisionController.emitirInternaBeta(factura);
    }

    @PostMapping("/local")
    public ComprobanteFirmaLocalService.ResultadoFirma firmarLocal(
            @Valid @RequestBody FacturaPruebaRequest factura
    ) {
        return firmaLocalService.firmar(factura);
    }
}
