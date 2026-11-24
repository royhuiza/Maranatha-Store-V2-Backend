package com.libreria.ventas.controller;

import com.libreria.ventas.service.ComprobanteVentaLocalService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/ventas")
public class ComprobanteVentaLocalController {

    private final ComprobanteVentaLocalService service;

    public ComprobanteVentaLocalController(
            ComprobanteVentaLocalService service) {
        this.service = service;
    }

    @GetMapping("/{id}/comprobante-local")
    public ComprobanteVentaLocalService.Vista consultar(
            @PathVariable Long id,
            HttpServletRequest request) {

        if (!request.isUserInRole("ADMIN")
                && !request.isUserInRole("ROLE_ADMIN")) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Acceso denegado"
            );
        }

        return service.consultar(id);
    }
}