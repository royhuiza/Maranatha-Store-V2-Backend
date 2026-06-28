package com.libreria.cliente.controller;

import com.libreria.cliente.model.Cliente;
import com.libreria.cliente.service.ClienteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/clientes")
@Tag(name = "Clientes", description = "Registro de personas y empresas")
public class ClienteController {

    private final ClienteService service;

    public ClienteController(ClienteService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar clientes")
    public List<Cliente> listarClientes() { return service.listarClientes(); }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID")
    public Cliente obtenerCliente(@PathVariable Long id) { return service.obtenerCliente(id); }

    @PostMapping
    @Operation(summary = "Registrar cliente")
    public ResponseEntity<Cliente> crearCliente(@Valid @RequestBody Cliente cliente) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearCliente(cliente));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar cliente")
    public Cliente actualizarCliente(@PathVariable Long id, @Valid @RequestBody Cliente cliente) {
        return service.actualizarCliente(id, cliente);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar cliente")
    public ResponseEntity<Void> eliminarCliente(@PathVariable Long id) {
        service.eliminarCliente(id);
        return ResponseEntity.noContent().build();
    }
}
