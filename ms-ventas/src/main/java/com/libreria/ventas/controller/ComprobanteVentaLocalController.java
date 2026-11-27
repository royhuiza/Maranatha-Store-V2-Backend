package com.libreria.ventas.controller;

import com.libreria.ventas.service.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ventas")
public class ComprobanteVentaLocalController {

    private final ComprobanteVentaLocalService service;
    private final EstadoComprobanteService estados;
    private final AccesoFiscalService acceso;

    public ComprobanteVentaLocalController(
            ComprobanteVentaLocalService service,
            EstadoComprobanteService estados,
            AccesoFiscalService acceso
    ) {
        this.service = service;
        this.estados = estados;
        this.acceso = acceso;
    }

    @GetMapping("/{id}/comprobante-local")
    public ComprobanteVentaLocalService.Vista consultar(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String token
    ) {

        acceso.exigirVendedorOAdmin(token);

        return service.consultar(id);
    }

    @GetMapping("/{id}/estado-sunat")
    public EstadoComprobanteService.Vista estado(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String token
    ) {

        acceso.exigirVendedorOAdmin(token);

        return estados.consultar(id);
    }

    // Recuperar la referencia del comprobante SUNAT Beta.
    @GetMapping("/{id}/comprobante-beta")
    public ComprobanteVentaLocalService.VistaBeta consultarBeta(
            @PathVariable Long id,
            @RequestHeader(
                    value = "Authorization",
                    required = false
            ) String token
    ) {

        acceso.exigirVendedorOAdmin(token);

        return service.consultarBeta(id);
    }
}