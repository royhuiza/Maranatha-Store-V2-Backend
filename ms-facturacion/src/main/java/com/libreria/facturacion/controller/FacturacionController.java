package com.libreria.facturacion.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/facturacion")
public class FacturacionController {

    @GetMapping("/estado")
    public Map<String, String> estado() {
        return Map.of(
                "servicio", "ms-facturacion",
                "estado", "ACTIVO",
                "ambiente", "SUNAT_BETA"
        );
    }
}
