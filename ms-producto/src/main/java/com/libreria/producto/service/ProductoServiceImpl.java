package com.libreria.producto.service;

import com.libreria.producto.client.VentasClient;
import com.libreria.producto.dto.AjusteStockRequest;
import com.libreria.producto.model.AjusteInventario;
import com.libreria.producto.model.Producto;
import com.libreria.producto.repository.AjusteInventarioRepository;
import com.libreria.producto.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repository;

    private final AjusteInventarioRepository ajustes;
    private final VentasClient ventas;
    private final JdbcTemplate jdbc;

    public ProductoServiceImpl(ProductoRepository repository, AjusteInventarioRepository ajustes,
                               VentasClient ventas, JdbcTemplate jdbc) {
        this.repository = repository;
        this.ajustes = ajustes;
        this.ventas = ventas;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listar(String consulta) {
        String termino = consulta.strip();
        if (termino.isEmpty()) return repository.findAll();
        if (termino.matches("\\d+")) {
            long id;
            try {
                id = Long.parseLong(termino);
            } catch (NumberFormatException ex) {
                return List.of();
            }
            return repository.findById(id).map(List::of).orElseGet(List::of);
        }
        return repository.findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(termino, termino);
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerProducto(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Override
    @Transactional
    public Producto crearProducto(Producto producto) {
        producto.setId(null);
        producto.setNombre(producto.getNombre().trim());
        producto.setDescripcion(normalizar(producto.getDescripcion()));
        producto.setPrecio(producto.getPrecio().setScale(2, RoundingMode.HALF_UP));
        if (producto.getActivo() == null) producto.setActivo(true);
        return repository.save(producto);
    }

    @Override
    @Transactional
    public Producto actualizarProducto(Long id, Producto cambios) {
        Producto producto = bloquearProducto(id);
        producto.setNombre(cambios.getNombre().trim());
        producto.setDescripcion(normalizar(cambios.getDescripcion()));
        producto.setPrecio(cambios.getPrecio().setScale(2, RoundingMode.HALF_UP));
        producto.setStock(cambios.getStock());
        if (cambios.getActivo() != null) producto.setActivo(cambios.getActivo());
        return repository.save(producto);
    }

    @Override
    @Transactional
    public Producto descontarStock(Long id, int cantidad) {
        Producto producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        validarSalida(producto, cantidad);
        producto.setStock(producto.getStock() - cantidad);
        return repository.save(producto);
    }

    private void validarSalida(Producto producto, int cantidad) {
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto está inactivo");
        }
        if (cantidad < 1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad debe ser positiva");
        if (producto.getStock() <= 5) throw new ResponseStatusException(HttpStatus.CONFLICT, "Stock bajo: no se permite vender productos con 5 unidades o menos");
        if (producto.getStock() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente para " + producto.getNombre() + ". Disponible: " + producto.getStock());
        }
    }

    @Override
    @Transactional
    public Producto reponerStock(Long id, int cantidad) {
        Producto producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        producto.setStock(stockResultante(producto, cantidad));
        return repository.save(producto);
    }

    @Override
    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = bloquearProducto(id);
        if (producto.getStock() > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto aún tiene stock. Ajusta sus existencias antes de eliminarlo.");
        }
        if (producto.salidasPorVenta() > 0 || ventas.tieneVentas(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto tiene ventas asociadas y no se puede eliminar.");
        }
        repository.delete(producto);
    }

    @Override
    @Transactional
    public Producto ajustarStock(Long id, AjusteStockRequest ajuste) {
        Producto producto = bloquearProducto(id);
        int anterior = producto.getStock();
        int cantidad = ajuste.tipo() == AjusteStockRequest.Tipo.REPONER ? ajuste.cantidad() : -ajuste.cantidad();
        int nuevo = stockResultante(producto, cantidad);
        producto.setStock(nuevo);
        ajustes.save(new AjusteInventario(id, ajuste.tipo(), ajuste.cantidad(), anterior, nuevo, ajuste.motivo().trim()));
        return repository.save(producto);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AjusteInventario> historialAjustes(Long id) {
        obtenerProducto(id);
        return ajustes.findByProductoIdOrderByIdDesc(id);
    }

    @Override
    @Transactional
    public void aplicarStockVenta(UUID idOperacion, Map<Long, Integer> cantidades, boolean reversa) {
        if (cantidades.isEmpty() || cantidades.entrySet().stream().anyMatch(e ->
                e.getKey() < 1 || e.getValue() == null || e.getValue() < 1)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indique cantidades positivas por producto.");
        }
        var ordenadas = new TreeMap<>(cantidades);
        // Una reversa que llega primero impide un descuento tardío con el mismo ID.
        int creada = jdbc.update("""
                insert into operaciones_stock_venta (id, cantidades, revertida) values (?, ?, ?)
                on conflict (id) do nothing
                """, idOperacion, ordenadas.toString(), reversa);
        if (creada == 1 && reversa) return;
        if (creada == 0) {
            var previa = jdbc.queryForMap("select cantidades, revertida from operaciones_stock_venta where id = ? for update", idOperacion);
            if (!ordenadas.toString().equals(previa.get("cantidades"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La operación de stock ya existe con otros artículos.");
            }
            if (Boolean.TRUE.equals(previa.get("revertida"))) {
                if (reversa) return;
                throw new ResponseStatusException(HttpStatus.CONFLICT, "La operación de stock ya fue revertida.");
            }
            if (!reversa) return;
            jdbc.update("update operaciones_stock_venta set revertida = true where id = ?", idOperacion);
        }
        // El mismo orden de bloqueo evita ciclos entre ventas concurrentes.
        for (var entrada : ordenadas.entrySet()) {
            Producto producto = bloquearProducto(entrada.getKey());
            int cantidad = entrada.getValue();
            if (reversa) {
                producto.setStock(stockResultante(producto, cantidad));
                producto.revertirSalidaVenta();
            } else {
                validarSalida(producto, cantidad);
                producto.setStock(producto.getStock() - cantidad);
                producto.registrarSalidaVenta();
            }
        }
    }

    private Producto bloquearProducto(Long id) {
        return repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private int stockResultante(Producto producto, int cambio) {
        long nuevo = (long) producto.getStock() + cambio;
        if (nuevo < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cantidad supera el stock disponible.");
        }
        if (nuevo > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El stock supera el máximo permitido.");
        }
        return (int) nuevo;
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
