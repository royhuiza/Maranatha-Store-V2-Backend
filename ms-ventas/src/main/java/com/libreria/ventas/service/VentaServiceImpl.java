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
import com.libreria.ventas.model.EstadoSunat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate yapeDb;
    private final ClienteClient clienteClient;
    private final ProductoClient productoClient;
    private final ObjectMapper objectMapper;
    private final VentaDetalleRepository detalles;
    private final VentaOperacionService ventaOperacionService;
    private final VentaRequestHashService ventaRequestHashService;
    private final VentaPersistenciaService ventaPersistenciaService;

    @org.springframework.beans.factory.annotation.Autowired
    private VentaFirmaAutomaticaService ventaFirmaAutomaticaService;

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(VentaServiceImpl.class);

    @org.springframework.beans.factory.annotation.Autowired
    public VentaServiceImpl(
            VentaRepository ventaRepository,
            ClienteClient clienteClient,
            ProductoClient productoClient,
            ObjectMapper objectMapper,
            VentaOperacionService ventaOperacionService,
            VentaRequestHashService ventaRequestHashService,
            VentaPersistenciaService ventaPersistenciaService,
            VentaDetalleRepository detalles){

        this.ventaRepository = ventaRepository;
        this.clienteClient = clienteClient;
        this.productoClient = productoClient;
        this.objectMapper = objectMapper;
        this.detalles = detalles;
        this.ventaOperacionService = ventaOperacionService;
        this.ventaRequestHashService = ventaRequestHashService;
        this.ventaPersistenciaService = ventaPersistenciaService;
    }

    // Constructor conservado para las pruebas de idempotencia existentes.
    public VentaServiceImpl(VentaRepository ventaRepository, ClienteClient clienteClient,
                            ProductoClient productoClient, ObjectMapper objectMapper,
                            VentaOperacionService ventaOperacionService, VentaRequestHashService ventaRequestHashService,
                            VentaPersistenciaService ventaPersistenciaService) {
        this(ventaRepository, clienteClient, productoClient, objectMapper,
                ventaOperacionService, ventaRequestHashService, ventaPersistenciaService, null);
    }

    private Venta verificarOperacionExistente(
            ResultadoReservaVenta reserva
    ) {

        if (reserva.nueva()) {
            return null;
        }

        VentaOperacion operacion = reserva.operacion();

        if (operacion.getEstado()
                == EstadoVentaOperacion.COMPLETADA) {

            if (operacion.getVentaId() == null) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Operacion completada sin referencia de venta"
                );
            }

            return ventaRepository
                    .findOneById(operacion.getVentaId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "No se encontro la venta registrada"
                            ));
        }

        // Recuperar una venta que ya fue guardada
        // antes de una interrupcion del servicio.

        Venta recuperada = recuperarVentaGuardada(
                operacion.getIdempotencyKey()
        );

        if (recuperada != null) {

            ventaOperacionService.completar(
                    operacion.getId(),
                    recuperada.getId()
            );

            return ventaRepository
                    .findOneById(recuperada.getId())
                    .orElseThrow(() ->
                            new ResponseStatusException(
                                    HttpStatus.CONFLICT,
                                    "No se pudo recuperar la venta"
                            ));
        }

        if (operacion.getEstado()
                == EstadoVentaOperacion.REQUIERE_REVISION) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operacion requiere revision de inventario"
            );
        }

        if (operacion.getEstado()
                == EstadoVentaOperacion.EN_PROCESO) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La operacion ya esta en proceso"
            );
        }

        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Estado de operacion no reconocido"
        );
    }


    private Venta intentarEmisionAutomatica(Venta venta) {

        if (venta == null
                || venta.getId() == null
                || ventaFirmaAutomaticaService == null) {
            return venta;
        }

        try {
            if (ventaFirmaAutomaticaService.isBetaHabilitado()) {
                ventaFirmaAutomaticaService.emitirVentaBeta(venta.getId());
            } else {
                ventaFirmaAutomaticaService.firmarVenta(venta.getId());
            }
        } catch (Exception ex) {
            log.warn(
                    "Emision automatica pendiente para venta {}. Error: {}",
                    venta.getId(),
                    ex.getClass().getSimpleName()
            );
        }

        if (ventaFirmaAutomaticaService.isBetaHabilitado()) {
            try {
                return ventaRepository.findOneById(venta.getId())
                        .orElse(venta);
            } catch (Exception ex) {
                log.warn("No se pudo recargar el estado fiscal de venta {}",
                        venta.getId());
            }
        }

        return venta;
    }

    private Venta recuperarVentaGuardada(String idempotencyKey) {

        return ventaPersistenciaService
                .buscarPorIdempotencyKey(idempotencyKey)
                .orElse(null);
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
    public Venta crearVenta(CrearVentaRequest request) {

        String clave = request.getIdempotencyKey();
        boolean yape = request.getMetodoPago() == MetodoPago.YAPE;
        if (yape ? !clave.matches("YAPE-[a-f0-9]{64}") : clave.startsWith("YAPE-")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Clave de pago Yape inválida");
        }

        String hash = ventaRequestHashService.calcular(request);

        ResultadoReservaVenta reserva =
                ventaOperacionService.reservarConResultado(
                        clave,
                        hash
                );

        Venta existente = verificarOperacionExistente(reserva);

        if (existente != null) {
            return intentarEmisionAutomatica(existente);
        }

        Long operacionId = reserva.operacion().getId();
        Venta guardada;

        try {

            ClienteDTO cliente =
                    obtenerYValidarCliente(request);

            LinkedHashMap<Long, Integer> cantidades =
                    agruparCantidades(request.getDetalles());

            LinkedHashMap<Long, ProductoDTO> productos =
                    consultarYValidarProductos(
                            cantidades,
                            request.getTipoComprobante()
                    );

            Venta venta = construirVenta(
                    request,
                    cliente,
                    cantidades,
                    productos
            );

            if (yape) {
                List<BigDecimal> montos = yapeDb.queryForList(
                        "SELECT monto FROM yape_evento WHERE id=? AND fecha >= ? AND fecha <= ?",
                        BigDecimal.class, clave.substring(5), System.currentTimeMillis()-600000,
                        System.currentTimeMillis()+300000);
                if (montos.size() != 1 || montos.get(0).compareTo(venta.getTotal()) != 0) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Pago Yape inexistente, vencido o importe distinto");
                }
                venta.setEstadoPago("DETECTADO");
            }

            for (Map.Entry<Long, Integer> entry :
                    cantidades.entrySet()) {

                String claveInventario =
                        "VENTA-" + operacionId
                                + "-" + entry.getKey();

                productoClient.descontarStockIdempotente(
                        entry.getKey(),
                        entry.getValue(),
                        claveInventario
                );
            }

            guardada =
                    ventaPersistenciaService.guardarVenta(
                            venta,
                            clave
                    );

            ventaOperacionService.completar(
                    operacionId,
                    guardada.getId()
            );

        } catch (Exception ex) {

            ventaOperacionService.marcarRevision(operacionId);

            if (ex instanceof ResponseStatusException error) {
                throw error;
            }

            if (ex instanceof ResourceAccessException) {

                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "Operacion pendiente de revision. "
                                + "No registre nuevamente la compra.",
                        ex
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "No se pudo confirmar la operacion. "
                            + "Se requiere verificar la venta "
                            + "y el inventario antes de reintentar.",
                    ex
            );
        }

        return intentarEmisionAutomatica(guardada);
    }

    @Override
    @Transactional
    public Venta anularVenta(Long id) {
        Venta venta = ventaRepository.findLockedById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Venta no encontrada"));
        if (venta.isValidadoSunat() || (venta.getEstadoSunat() != null
                && venta.getEstadoSunat() != EstadoSunat.PENDIENTE
                && venta.getEstadoSunat() != EstadoSunat.NO_APLICA
                && venta.getEstadoSunat() != EstadoSunat.ACEPTADO_BETA)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Un comprobante fiscal requiere su proceso de baja o nota de credito");
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
        if (venta.isValidadoSunat() || (venta.getEstadoSunat() != null
                && venta.getEstadoSunat() != EstadoSunat.PENDIENTE
                && venta.getEstadoSunat() != EstadoSunat.NO_APLICA)) throw new ResponseStatusException(HttpStatus.CONFLICT,"No se permite eliminar comprobantes fiscales");
        if (venta.getEstadoPago() != null && !"PENDIENTE".equals(venta.getEstadoPago())) throw new ResponseStatusException(HttpStatus.CONFLICT,"No se permite eliminar una venta con pago registrado");
        if (venta.getEstado() != EstadoVenta.ANULADA) throw new ResponseStatusException(HttpStatus.CONFLICT,"Anule la venta antes de eliminarla para reponer su stock una sola vez");
        if (venta.getIdempotencyKey() != null) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Conserve la venta anulada para mantener la trazabilidad");
        ventaRepository.delete(venta);
        ventaRepository.flush();
    }

    private ClienteDTO obtenerYValidarCliente(CrearVentaRequest request) {

        ClienteDTO cliente = null;

        String documento = request.getDocumento() == null ? "" : request.getDocumento().trim();
        String numeroExplicito = Objects.toString(request.getNumeroDocumento(), "").trim();
        if (!documento.isEmpty() && !numeroExplicito.isEmpty() && !documento.equals(numeroExplicito)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los documentos del comprador no coinciden");
        }
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
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "El cliente seleccionado no existe", ex);
            } catch (ResourceAccessException ex) {
                throw new ResponseStatusException(
                        HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo comunicar con el servicio de clientes", ex);
            }
        }
        if (cliente == null) {
            String tipo = Objects.toString(
                    request.getTipoDocumento(), "").trim().toUpperCase(Locale.ROOT);

            String numero = Objects.toString(
                    request.getNumeroDocumento(), "").trim();

            String nombre = Objects.toString(
                    request.getClienteNombre(), "").trim();

            if (!numero.isBlank() || !nombre.isBlank() || !tipo.isBlank()) {

                if (nombre.isBlank()) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Ingrese el nombre del comprador");
                }

                boolean documentoValido =
                        ("DNI".equals(tipo) && numero.matches("\\d{8}"))
                                || ("RUC".equals(tipo) && numero.matches("\\d{11}"));

                if (!documentoValido) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Documento no válido");
                }

                cliente = new ClienteDTO();
                cliente.setNombre(nombre);
                cliente.setTipoDocumento(tipo);
                cliente.setNumeroDocumento(numero);
            }
        }

        if (cliente != null && ((!documento.isEmpty() && !documento.equals(cliente.getNumeroDocumento()))
                || (!numeroExplicito.isEmpty() && !numeroExplicito.equals(cliente.getNumeroDocumento()))
                || (request.getClienteId() != null && !request.getClienteId().equals(cliente.getId())))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos del cliente no coinciden");
        }
        if (request.getTipoComprobante() == TipoComprobante.FACTURA) {
            if (cliente == null
                    || !"RUC".equalsIgnoreCase(cliente.getTipoDocumento())
                    || cliente.getNumeroDocumento() == null
                    || !cliente.getNumeroDocumento().matches("\\d{11}")) {

                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "La factura requiere un RUC de 11 dígitos");
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

    private LinkedHashMap<Long, ProductoDTO> consultarYValidarProductos(
            Map<Long, Integer> cantidades, TipoComprobante tipoComprobante) {
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
                // Las facturas y boletas requieren una clasificación tributaria conocida.
                // Las notas de pedido conservan su comportamiento anterior.
                if (tipoComprobante != TipoComprobante.NOTA_PEDIDO) {
                    String afectacion = producto.getAfectacionIgv();
                    if (afectacion == null ||
                            !Set.of("GRAVADO", "EXONERADO", "INAFECTO").contains(afectacion)) {
                        throw new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "El producto " + producto.getNombre()
                                        + " no tiene una clasificación tributaria válida"
                        );
                    }
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
        venta.setEstadoSunat(
                request.getTipoComprobante() == TipoComprobante.NOTA_PEDIDO
                        ? EstadoSunat.NO_APLICA
                        : EstadoSunat.PENDIENTE
        );

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
            detalle.setAfectacionIgv(producto.getAfectacionIgv());
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
