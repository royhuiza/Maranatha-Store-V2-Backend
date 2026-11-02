package com.libreria.producto.service;

import com.libreria.producto.model.Producto;
import com.libreria.producto.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.RoundingMode;
import java.util.List;
import com.libreria.producto.model.TipoAfectacionIgv;

@Service
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository repository;

    public ProductoServiceImpl(ProductoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Producto obtenerProducto(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    @Override
    @Transactional
    public Producto actualizarAfectacionIgv(
            Long id,
            TipoAfectacionIgv afectacionIgv) {

        if (afectacionIgv == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La clasificacion tributaria es obligatoria"
            );
        }

        Producto producto = obtenerProducto(id);
        producto.setAfectacionIgv(afectacionIgv);

        return repository.save(producto);
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

        Producto producto = obtenerProducto(id);

        producto.setNombre(cambios.getNombre().trim());

        producto.setDescripcion(
                normalizar(cambios.getDescripcion())
        );

        producto.setPrecio(
                cambios.getPrecio().setScale(2, RoundingMode.HALF_UP)
        );

        producto.setStock(cambios.getStock());

        if (cambios.getActivo() != null) {
            producto.setActivo(cambios.getActivo());
        }

        // Actualizar la clasificación tributaria únicamente
        // cuando el cliente envíe un valor.
        if (cambios.getAfectacionIgv() != null) {
            producto.setAfectacionIgv(
                    cambios.getAfectacionIgv()
            );
        }

        return repository.save(producto);
    }


    @Override
    @Transactional
    public Producto descontarStock(Long id, int cantidad) {
        Producto producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El producto está inactivo");
        }
        if (producto.getStock() < cantidad) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Stock insuficiente para " + producto.getNombre() + ". Disponible: " + producto.getStock());
        }
        producto.setStock(producto.getStock() - cantidad);
        return repository.save(producto);
    }

    @Override
    @Transactional
    public Producto reponerStock(Long id, int cantidad) {
        Producto producto = repository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
        producto.setStock(producto.getStock() + cantidad);
        return repository.save(producto);
    }

    @Override
    @Transactional
    public void eliminarProducto(Long id) {
        Producto producto = obtenerProducto(id);
        repository.delete(producto);
    }

    private String normalizar(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
