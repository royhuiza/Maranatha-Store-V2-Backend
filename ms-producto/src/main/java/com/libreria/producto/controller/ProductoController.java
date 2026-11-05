package com.libreria.producto.controller;

import com.libreria.producto.dto.AjusteStockRequest;
import com.libreria.producto.dto.StockRequest;
import com.libreria.producto.model.AjusteInventario;
import com.libreria.producto.service.ImportacionProductos;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.multipart.MultipartFile;
import java.util.Map;
import java.util.UUID;
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

    private final ImportacionProductos importacion;

    public ProductoController(ProductoService service, ImportacionProductos importacion) {
        this.service = service;
        this.importacion = importacion;
    }

    @GetMapping
    @Operation(summary = "Listar o buscar productos por nombre, descripción o ID exacto")
    public List<Producto> listar(@RequestParam(name = "q", defaultValue = "") String consulta) {
        return service.listar(consulta);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar producto por ID")
    public Producto obtener(@PathVariable Long id) { return service.obtenerProducto(id); }

    @PostMapping
    @Operation(summary = "Registrar producto")
    public ResponseEntity<Producto> crear(@Validated(Producto.DatosCatalogo.class) @RequestBody Producto producto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crearProducto(producto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public Producto actualizar(@PathVariable Long id, @Validated(Producto.DatosCatalogo.class) @RequestBody Producto producto) {
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

    @PostMapping(path = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportacionProductos.Resultado importar(@RequestParam("archivo") MultipartFile archivo) {
        return importacion.importar(archivo);
    }

    @PostMapping("/{id}/ajustes")
    public Producto ajustar(@PathVariable Long id, @Valid @RequestBody AjusteStockRequest ajuste) {
        return service.ajustarStock(id, ajuste);
    }

    @GetMapping("/{id}/ajustes")
    public List<AjusteInventario> historial(@PathVariable Long id) {
        return service.historialAjustes(id);
    }

    @PostMapping("/stock/venta/{idOperacion}/{operacion:descontar|revertir}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stockVenta(@PathVariable UUID idOperacion, @PathVariable String operacion,
                           @RequestBody Map<Long, Integer> cantidades,
                           @RequestAttribute(value = "stockInterno", required = false) Boolean stockInterno) {
        if (!Boolean.TRUE.equals(stockInterno)) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        service.aplicarStockVenta(idOperacion, cantidades, operacion.equals("revertir"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminarProducto(id);
        return ResponseEntity.noContent().build();
    }
}
