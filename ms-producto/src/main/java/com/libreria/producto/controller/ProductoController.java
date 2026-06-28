package com.libreria.producto.controller;

import com.libreria.producto.dto.StockRequest;
import com.libreria.producto.model.Producto;
import com.libreria.producto.service.ProductoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "Inventario y control de stock")
public class ProductoController {

    private final ProductoService service;

    public ProductoController(ProductoService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar productos")
    public List<Producto> listar() { return service.listar(); }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar producto por ID")
    public Producto obtener(@PathVariable Long id) { return service.obtenerProducto(id); }

    @PostMapping
    @Operation(summary = "Registrar producto")
    public ResponseEntity<Producto> crear(@Valid @RequestBody Producto producto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearProducto(producto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public Producto actualizar(@PathVariable Long id, @Valid @RequestBody Producto producto) {
        return service.actualizarProducto(id, producto);
    }

    @PostMapping("/{id}/stock/descontar")
    @Operation(summary = "Descontar stock de forma segura")
    public Producto descontar(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return service.descontarStock(id, request.getCantidad());
    }

    @PostMapping("/{id}/stock/reponer")
    @Operation(summary = "Reponer stock")
    public Producto reponer(@PathVariable Long id, @Valid @RequestBody StockRequest request) {
        return service.reponerStock(id, request.getCantidad());
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
