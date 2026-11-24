
package com.libreria.producto.controller;

import com.libreria.producto.dto.AfectacionIgvRequest;
import com.libreria.producto.dto.AjusteStockRequest;
import com.libreria.producto.dto.StockRequest;
import com.libreria.producto.dto.StockIdempotenteRequest;

import com.libreria.producto.model.AjusteInventario;
import com.libreria.producto.model.Producto;
import com.libreria.producto.model.InventarioOperacion;
import com.libreria.producto.model.TipoInventarioOperacion;

import com.libreria.producto.repository.InventarioOperacionRepository;

import com.libreria.producto.service.ImportacionProductos;
import com.libreria.producto.service.ProductoService;
import com.libreria.producto.service.InventarioIdempotenteService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/productos")
@Tag(name = "Productos", description = "Inventario y control de stock")
public class ProductoController {

    private final ProductoService service;
    private final ImportacionProductos importacion;
    private final InventarioIdempotenteService inventarioIdempotenteService;
    private final InventarioOperacionRepository inventarioOperacionRepository;

    public ProductoController(
            ProductoService service,
            ImportacionProductos importacion,
            InventarioIdempotenteService inventarioIdempotenteService,
            InventarioOperacionRepository inventarioOperacionRepository) {

        this.service = service;
        this.importacion = importacion;
        this.inventarioIdempotenteService = inventarioIdempotenteService;
        this.inventarioOperacionRepository = inventarioOperacionRepository;
    }

    @GetMapping
    @Operation(summary = "Listar o buscar productos por nombre, descripción o ID exacto")
    public List<Producto> listar(
            @RequestParam(name = "q", defaultValue = "") String consulta) {

        return service.listar(consulta);
    }

    @GetMapping("/stock-bajo")
    @Operation(summary = "Listar productos activos con stock bajo o agotado")
    public List<Producto> stockBajo() {
        return service.listarStockBajo();
    }

    @GetMapping("/{id:[0-9]+}")
    @Operation(summary = "Buscar producto por ID")
    public Producto obtener(@PathVariable Long id) {
        return service.obtenerProducto(id);
    }

    @PostMapping
    @Operation(summary = "Registrar producto")
    public ResponseEntity<Producto> crear(
            @Validated(Producto.DatosCatalogo.class)
            @RequestBody Producto producto) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.crearProducto(producto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto")
    public Producto actualizar(
            @PathVariable Long id,
            @Validated(Producto.DatosCatalogo.class)
            @RequestBody Producto producto) {

        return service.actualizarProducto(id, producto);
    }

    @PostMapping("/{id}/stock/descontar")
    @Operation(summary = "Descontar stock de forma segura")
    public Producto descontar(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {

        return service.descontarStock(id, request.getCantidad());
    }

    @PostMapping("/{id}/stock/reponer")
    @Operation(summary = "Reponer stock")
    public Producto reponer(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {

        return service.reponerStock(id, request.getCantidad());
    }

    @PostMapping("/{id}/stock/descontar-idempotente")
    @Operation(summary = "Descontar stock una sola vez por clave de operación")
    public Producto descontarIdempotente(
            @PathVariable Long id,
            @Valid @RequestBody StockIdempotenteRequest request) {

        return inventarioIdempotenteService.descontar(
                id,
                request.getCantidad(),
                request.getOperationKey()
        );
    }

    @PostMapping("/{id}/stock/reponer-idempotente")
    @Operation(summary = "Reponer stock una sola vez por clave de operación")
    public Producto reponerIdempotente(
            @PathVariable Long id,
            @Valid @RequestBody StockIdempotenteRequest request) {

        return inventarioIdempotenteService.reponer(
                id,
                request.getCantidad(),
                request.getOperationKey(),
                request.getOperacionOrigenKey()
        );
    }

    @GetMapping("/inventario/operaciones/{operationKey}")
    @Operation(summary = "Consultar operación de inventario")
    public InventarioOperacion consultarOperacionInventario(
            @PathVariable String operationKey) {

        if (operationKey == null
                || operationKey.isBlank()
                || operationKey.length() > 150) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Clave de operación inválida"
            );
        }

        return inventarioOperacionRepository
                .findByOperationKey(operationKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Operación de inventario no encontrada"
                ));
    }

    @GetMapping("/inventario/operaciones/{operationKey}/reposicion")
    @Operation(summary = "Consultar reposición asociada a un descuento")
    public InventarioOperacion consultarReposicionDeDescuento(
            @PathVariable String operationKey) {

        if (operationKey == null
                || operationKey.isBlank()
                || operationKey.length() > 150) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Clave de descuento inválida"
            );
        }

        return inventarioOperacionRepository
                .findByTipoAndOperacionOrigenKey(
                        TipoInventarioOperacion.REPOSICION,
                        operationKey
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró reposición para este descuento"
                ));
    }

    @PostMapping(
            path = "/importar",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @Operation(summary = "Importar productos desde Excel")
    public ImportacionProductos.Resultado importar(
            @RequestParam("archivo") MultipartFile archivo) {

        return importacion.importar(archivo);
    }

    @PostMapping("/{id}/ajustes")
    @Operation(summary = "Realizar ajuste de inventario")
    public Producto ajustar(
            @PathVariable Long id,
            @Valid @RequestBody AjusteStockRequest ajuste) {

        return service.ajustarStock(id, ajuste);
    }

    @GetMapping("/{id}/ajustes")
    @Operation(summary = "Consultar historial de ajustes de inventario")
    public List<AjusteInventario> historial(@PathVariable Long id) {
        return service.historialAjustes(id);
    }

    @PostMapping("/stock/venta/{idOperacion}/{operacion:descontar|revertir}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stockVenta(
            @PathVariable UUID idOperacion,
            @PathVariable String operacion,
            @RequestBody Map<Long, Integer> cantidades,
            @RequestAttribute(
                    value = "stockInterno",
                    required = false
            ) Boolean stockInterno) {

        if (!Boolean.TRUE.equals(stockInterno)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        service.aplicarStockVenta(
                idOperacion,
                cantidades,
                operacion.equals("revertir")
        );
    }

    @PatchMapping("/{id}/afectacion-igv")
    @Operation(summary = "Actualizar clasificación tributaria del producto")
    public Producto actualizarAfectacionIgv(
            @PathVariable Long id,
            @Valid @RequestBody AfectacionIgvRequest request) {

        return service.actualizarAfectacionIgv(
                id,
                request.afectacionIgv()
        );
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar producto")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {

        service.eliminarProducto(id);

        return ResponseEntity.noContent().build();
    }
}
