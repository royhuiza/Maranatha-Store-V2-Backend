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
    @org.springframework.beans.factory.annotation.Value("${maranatha.demo.checkout.enabled:false}")
    private boolean checkoutDemo;
    @org.springframework.beans.factory.annotation.Value("${maranatha.demo.integrado.enabled:false}")
    private boolean demoIntegrado;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<com.libreria.ventas.service.CheckoutDemoIntegrado> checkoutIntegrado;
    @org.springframework.beans.factory.annotation.Autowired
    private com.libreria.ventas.service.AccesoFiscalService accesoFiscal;
    @org.springframework.beans.factory.annotation.Value("${maranatha.laboratorio.aislado:false}")
    private boolean laboratorioAislado;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.beans.factory.ObjectProvider<com.libreria.ventas.service.VentaLaboratorioService> servicioDemo;
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
    public List<Venta> listarVentas(@RequestHeader(value="Authorization",required=false) String token) {
        if(demoIntegrado)accesoFiscal.exigirAdmin(token);
        var lista=ventaService.listarVentas();
        if(demoIntegrado)checkoutIntegrado.getObject().adjuntarCompradores(lista);
        return lista;
    }

    @GetMapping("/historial")
    @Operation(summary = "Historial filtrado por fechas (aaaa-mm-dd) y número de comprobante, paginado")
    public HistorialVentas historial(@RequestParam(required = false) String desde,
                                     @RequestParam(required = false) String hasta,
                                     @RequestParam(required = false) String numero,
                                     @RequestParam(defaultValue = "0") int pagina,
                                     @RequestParam(defaultValue = "10") int tamano,
                                     @RequestHeader(value="Authorization",required=false) String token) {
        if(demoIntegrado)accesoFiscal.exigirAdmin(token);
        var paginaVentas=historialService.consultar(desde, hasta, numero, pagina, tamano);
        if(demoIntegrado)checkoutIntegrado.getObject().adjuntarCompradores(paginaVentas.contenido());
        return paginaVentas;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener una venta y su comprobante")
    public Venta obtenerVenta(@PathVariable Long id,@RequestHeader(value="Authorization",required=false) String token) {
        if(demoIntegrado)accesoFiscal.exigirAdmin(token);
        var v=ventaService.obtenerVenta(id);
        return demoIntegrado?checkoutIntegrado.getObject().adjuntarComprador(v):v;
    }

    @PostMapping
    @Operation(summary = "Registrar venta, calcular total y descontar stock")
    public ResponseEntity<Venta> crearVenta(@Valid @RequestBody CrearVentaRequest request, @RequestHeader(value="Authorization",required=false) String token) {
        if (demoIntegrado) {
            if(!laboratorioAislado||checkoutIntegrado.getIfAvailable()==null) throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Instancia demo no aislada");
            accesoFiscal.exigirAdmin(token);
            Venta v=checkoutIntegrado.getObject().confirmar(request);
            boolean completo=v.getEstadoSunat()==com.libreria.ventas.model.EstadoSunat.ACEPTADO_BETA || v.getEstadoSunat()==com.libreria.ventas.model.EstadoSunat.NO_APLICA;
            return ResponseEntity.status(completo?HttpStatus.CREATED:HttpStatus.ACCEPTED).body(v);
        }
        if (checkoutDemo) {
            if (!laboratorioAislado || servicioDemo.getIfAvailable() == null)
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Checkout Beta exige instancia aislada");
            Venta v = servicioDemo.getObject().confirmarDemo(request);
            // HTTP 202 significa registrada, pero no aceptada: nunca confirmar de nuevo con otra clave.
            return ResponseEntity.status(v.getEstadoSunat() == com.libreria.ventas.model.EstadoSunat.ACEPTADO_BETA ? HttpStatus.CREATED : HttpStatus.ACCEPTED).body(v);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ventaService.crearVenta(request));
    }

    @PutMapping("/{id}/anular")
    @Operation(summary = "Anular venta y reponer stock")
    public Venta anularVenta(@PathVariable Long id) {
        if(demoIntegrado){if(!laboratorioAislado||checkoutIntegrado.getIfAvailable()==null)throw new org.springframework.web.server.ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Demo no aislada");return checkoutIntegrado.getObject().adjuntarComprador(checkoutIntegrado.getObject().anular(id));}
        return ventaService.anularVenta(id);
    }

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
