package com.libreria.cliente.controller;
import com.libreria.cliente.service.DecolectaService;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/clientes/documentos")
public class DocumentoController {
 private final DecolectaService service;
 public DocumentoController(DecolectaService service) { this.service=service; }
 @GetMapping("/dni/{numero}")
 public DecolectaService.Documento dni(@PathVariable String numero) { return service.consultar("DNI",numero); }
 @GetMapping("/ruc/{numero}")
 public DecolectaService.Documento ruc(@PathVariable String numero) { return service.consultar("RUC",numero); }
}
