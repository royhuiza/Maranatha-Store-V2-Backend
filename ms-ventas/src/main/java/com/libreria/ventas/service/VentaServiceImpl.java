package com.libreria.ventas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.libreria.ventas.client.ClienteClient;
import com.libreria.ventas.client.ProductoClient;
import com.libreria.ventas.dto.ClienteDTO;
import com.libreria.ventas.dto.CrearVentaRequest;
import com.libreria.ventas.dto.DetalleVentaRequest;
import com.libreria.ventas.dto.ProductoDTO;
import com.libreria.ventas.model.*;
import com.libreria.ventas.repository.VentaRepository;
import com.libreria.ventas.repository.VentaDetalleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ClienteClient clienteClient;
    private final ProductoClient productoClient;
    private final ObjectMapper objectMapper;
    private final VentaDetalleRepository detalles;

    public VentaServiceImpl(VentaRepository ventaRepository, ClienteClient clienteClient,
                            ProductoClient productoClient, ObjectMapper objectMapper, VentaDetalleRepository detalles) {
        this.ventaRepository = ventaRepository;
        this.clienteClient = clienteClient;
        this.productoClient = productoClient;
        this.objectMapper = objectMapper;
        this.detalles = detalles;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Venta> listarVentas() {
        return ventaRepository.findAllByOrderByFechaDesc();
    }

    @Override
    @Transactional(readOnly = true)
    public Venta obtenerVenta(Long id) {
        return ventaRepository.findOneById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
    }

    @Override
    @Transactional
    public Venta crearVenta(CrearVentaRequest request) {
        ClienteDTO cliente = obtenerYValidarCliente(request);
        LinkedHashMap<Long, Integer> cantidades = agruparCantidades(request.getDetalles());
        LinkedHashMap<Long, ProductoDTO> productos = consultarYValidarProductos(cantidades);

        Venta venta = construirVenta(request, cliente, cantidades, productos);
        UUID idOperacionStock = UUID.randomUUID();
        boolean stockDescontado = false;
        try {
            productoClient.aplicarStockVenta(idOperacionStock, cantidades, false);
            stockDescontado = true;

            ventaRepository.saveAndFlush(venta);
            venta.setNumero(String.format("%08d", venta.getId()));
            return ventaRepository.saveAndFlush(venta);
        } catch (Exception ex) {
            if (stockDescontado || ex instanceof ResourceAccessException) {
                try {
                    productoClient.aplicarStockVenta(idOperacionStock, cantidades, true);
                } catch (RuntimeException falloReposicion) {
                    ex.addSuppressed(falloReposicion);
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "La venta no se guardó y no se pudo confirmar la reposición. Revise el inventario.", ex);
                }
            }
            if (ex instanceof ResponseStatusException responseStatusException) {
                throw responseStatusException;
            }
            if (ex instanceof HttpStatusCodeException httpException) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, mensajeRemoto(httpException), ex);
            }
            if (ex instanceof ResourceAccessException) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo comunicar con el servicio de productos", ex);
            }
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "No se pudo guardar la venta; el stock fue restaurado", ex);
        }
    }

    @Override
    @Transactional
    public Venta anularVenta(Long id) {
        Venta venta = ventaRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (venta.isValidadoSunat()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Un comprobante fiscal requiere su proceso de baja o nota de credito");
        if (venta.getEstadoPago() != null && !"PENDIENTE".equals(venta.getEstadoPago())) throw new ResponseStatusException(HttpStatus.CONFLICT, "Revise el pago antes de anular esta venta");
        if (venta.getEstado() == EstadoVenta.ANULADA) return venta;

        List<VentaDetalle> repuestos = new ArrayList<>();
        try {
            for (VentaDetalle detalle : venta.getDetalles()) {
                productoClient.reponerStock(detalle.getProductoId(), detalle.getCantidad());
                repuestos.add(detalle);
            }
            venta.setEstado(EstadoVenta.ANULADA);
            return ventaRepository.save(venta);
        } catch (Exception ex) {
            // Si una reposición falla, vuelve a descontar lo ya repuesto para mantener consistencia.
            for (VentaDetalle detalle : repuestos) {
                try { productoClient.descontarStock(detalle.getProductoId(), detalle.getCantidad()); }
                catch (Exception ignored) { }
            }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "No se pudo anular la venta porque el servicio de inventario no está disponible", ex);
        }
    }

    @Override
    @Transactional
    public void eliminarVenta(Long id) {
        Venta venta = ventaRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (venta.isValidadoSunat()) throw new ResponseStatusException(HttpStatus.CONFLICT,"No se permite eliminar comprobantes fiscales");
        if (venta.getEstadoPago() != null && !"PENDIENTE".equals(venta.getEstadoPago())) throw new ResponseStatusException(HttpStatus.CONFLICT,"No se permite eliminar una venta con pago registrado");
        if (venta.getEstado() != EstadoVenta.ANULADA) throw new ResponseStatusException(HttpStatus.CONFLICT,"Anule la venta antes de eliminarla para reponer su stock una sola vez");
        ventaRepository.delete(venta);
        ventaRepository.flush();
    }

    private ClienteDTO obtenerYValidarCliente(CrearVentaRequest request) {
        ClienteDTO cliente = null;

        String documento = request.getDocumento() == null ? "" : request.getDocumento().trim();
        if (!documento.isEmpty()) {
            String tipo = documento.length() == 11 ? "RUC" : "DNI";
            try {
                cliente = clienteClient.obtenerClientePorDocumento(tipo, documento);
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound ex) {
                cliente = null;
            } catch (org.springframework.web.client.HttpClientErrorException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "No se pudo validar el documento " + documento, ex);
            } catch (org.springframework.web.client.ResourceAccessException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo comunicar con el servicio de clientes", ex);
            }
        }

        if (cliente == null && request.getClienteId() != null) {
            try {
                cliente = clienteClient.obtenerCliente(request.getClienteId());
            } catch (HttpStatusCodeException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El cliente seleccionado no existe", ex);
            } catch (ResourceAccessException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo comunicar con el servicio de clientes", ex);
            }
        }

        if (request.getTipoComprobante() == TipoComprobante.FACTURA) {
            if (cliente == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para emitir una factura debe seleccionar un cliente");
            }
            if (!"RUC".equalsIgnoreCase(cliente.getTipoDocumento())
                    || cliente.getNumeroDocumento() == null
                    || !cliente.getNumeroDocumento().matches("\\d{11}")) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Para emitir una factura el cliente debe tener un RUC válido de 11 dígitos");
            }
        }
        return cliente;
    }

    private LinkedHashMap<Long, Integer> agruparCantidades(List<DetalleVentaRequest> detalles) {
        LinkedHashMap<Long, Integer> cantidades = new LinkedHashMap<>();
        for (DetalleVentaRequest detalle : detalles) {
            try {
                cantidades.merge(detalle.getProductoId(), detalle.getCantidad(), Math::addExact);
            } catch (ArithmeticException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cantidad de producto demasiado grande");
            }
        }
        return cantidades;
    }

    private LinkedHashMap<Long, ProductoDTO> consultarYValidarProductos(Map<Long, Integer> cantidades) {
        LinkedHashMap<Long, ProductoDTO> productos = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            try {
                ProductoDTO producto = productoClient.obtenerProducto(entry.getKey());
                if (producto == null || producto.getPrecio() == null || producto.getStock() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Respuesta inválida del servicio de productos");
                }
                if (!Boolean.TRUE.equals(producto.getActivo())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "El producto " + producto.getNombre() + " está inactivo");
                }
                if (producto.getStock() <= 0) throw new ResponseStatusException(HttpStatus.CONFLICT, "Sin stock: " + producto.getNombre() + " no tiene unidades disponibles");
                if (producto.getStock() < entry.getValue()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Stock insuficiente para " + producto.getNombre() + ". Disponible: " + producto.getStock());
                }
                productos.put(entry.getKey(), producto);
            } catch (HttpStatusCodeException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeRemoto(ex), ex);
            } catch (ResourceAccessException ex) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo comunicar con el servicio de productos", ex);
            }
        }
        return productos;
    }

    private Venta construirVenta(CrearVentaRequest request, ClienteDTO cliente,
                                  LinkedHashMap<Long, Integer> cantidades,
                                  LinkedHashMap<Long, ProductoDTO> productos) {
        Venta venta = new Venta();
        venta.setClienteId(cliente == null ? null : cliente.getId());
        venta.setClienteNombre(cliente == null ? "Público general" : cliente.nombreCompleto().trim());
        venta.setClienteDocumento(cliente == null || cliente.getNumeroDocumento() == null ? null
                : cliente.getTipoDocumento() + " " + cliente.getNumeroDocumento());
        String documento = request.getDocumento() == null ? "" : request.getDocumento().trim();
        if (venta.getClienteDocumento() == null && !documento.isEmpty()) {
            venta.setClienteDocumento((documento.length() == 11 ? "RUC " : "DNI ") + documento);
        }
        venta.setFecha(LocalDateTime.now(PeruTime.ZONE));
        venta.setFechaZona(PeruTime.ZONE.getId());
        venta.setTipoComprobante(request.getTipoComprobante());
        venta.setMetodoPago(request.getMetodoPago());
        venta.setSerie(seriePara(request.getTipoComprobante()));
        venta.setNumero("PENDIENTE");
        venta.setEstado(EstadoVenta.EMITIDA);

        BigDecimal total = BigDecimal.ZERO;
        for (Map.Entry<Long, Integer> entry : cantidades.entrySet()) {
            ProductoDTO producto = productos.get(entry.getKey());
            BigDecimal precio = producto.getPrecio().setScale(2, RoundingMode.HALF_UP);
            BigDecimal subtotal = precio.multiply(BigDecimal.valueOf(entry.getValue()))
                    .setScale(2, RoundingMode.HALF_UP);
            VentaDetalle detalle = new VentaDetalle();
            detalle.setProductoId(producto.getId());
            detalle.setProductoNombre(producto.getNombre());
            detalle.setCantidad(entry.getValue());
            detalle.setPrecioUnitario(precio);
            detalle.setSubtotal(subtotal);
            venta.agregarDetalle(detalle);
            total = total.add(subtotal);
        }
        venta.setTotal(total.setScale(2, RoundingMode.HALF_UP));
        return venta;
    }

    private String seriePara(TipoComprobante tipo) {
        return switch (tipo) {
            case FACTURA -> "F001";
            case NOTA_PEDIDO -> "NP01";
            case BOLETA -> "B001";
        };
    }

    @Override
    @Transactional(readOnly = true)
    public boolean tieneVentas(Long productoId) {
        return detalles.existsByProductoId(productoId);
    }

    private String mensajeRemoto(HttpStatusCodeException ex) {
        try {
            JsonNode node = objectMapper.readTree(ex.getResponseBodyAsString());
            if (node.hasNonNull("message")) return node.get("message").asText();
        } catch (Exception ignored) { }
        return "La operación fue rechazada por un servicio relacionado";
    }
}
