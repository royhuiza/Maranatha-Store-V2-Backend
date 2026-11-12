package com.libreria.ventas.controller;

import com.libreria.ventas.client.ClienteClient;
import com.libreria.ventas.client.ProductoClient;
import com.libreria.ventas.dto.ClienteDTO;
import com.libreria.ventas.dto.CrearVentaRequest;
import com.libreria.ventas.dto.HistorialVentas;
import com.libreria.ventas.service.HistorialVentaService;
import com.libreria.ventas.dto.ProductoDTO;
import com.libreria.ventas.model.Venta;
import com.libreria.ventas.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/ventas")
@Tag(name = "Ventas y comprobantes")
public class VentaController {

    private final VentaService ventaService;
    private final ClienteClient clienteClient;
    private final ProductoClient productoClient;
    private final HistorialVentaService historialService;

    public VentaController(VentaService ventaService, ClienteClient clienteClient, ProductoClient productoClient,
                           HistorialVentaService historialService) {
        this.ventaService = ventaService;
        this.historialService = historialService;
        this.clienteClient = clienteClient;
        this.productoClient = productoClient;
    }

    @GetMapping
    @Operation(summary = "Listar ventas")
    public List<Venta> listarVentas() { return ventaService.listarVentas(); }

    @GetMapping("/historial")
    @Operation(summary = "Historial filtrado por fechas (aaaa-mm-dd) y número de comprobante, paginado")
    public HistorialVentas historial(@RequestParam(required = false) String desde,
                                     @RequestParam(required = false) String hasta,
                                     @RequestParam(required = false) String numero,
                                     @RequestParam(defaultValue = "0") int pagina,
                                     @RequestParam(defaultValue = "10") int tamano) {
        return historialService.consultar(desde, hasta, numero, pagina, tamano);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una venta y su comprobante")
    public Venta obtenerVenta(@PathVariable Long id) { return ventaService.obtenerVenta(id); }

    @PostMapping
    @Operation(summary = "Registrar venta, calcular total y descontar stock")
    public ResponseEntity<Venta> crearVenta(@Valid @RequestBody CrearVentaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crearVenta(request));
    }

    @PutMapping("/{id}/anular")
    @Operation(summary = "Anular venta y reponer stock")
    public Venta anularVenta(@PathVariable Long id) { return ventaService.anularVenta(id); }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Eliminar un registro interno anulado")
    public void eliminarVenta(@PathVariable Long id) { ventaService.eliminarVenta(id); }

    @GetMapping("/cliente/{clienteId}")
    @Operation(summary = "Comprobar comunicación con clientes")
    public ClienteDTO obtenerCliente(@PathVariable Long clienteId) {
        return clienteClient.obtenerCliente(clienteId);
    }

    @GetMapping("/producto/{productoId}/asociado")
    public boolean tieneVentas(@PathVariable Long productoId) {
        return ventaService.tieneVentas(productoId);
    }

    @GetMapping("/producto/{productoId}")
    @Operation(summary = "Comprobar comunicación con productos")
    public ProductoDTO obtenerProducto(@PathVariable Long productoId) {
        return productoClient.obtenerProducto(productoId);
    }
}
