package com.libreria.producto.service;

import com.libreria.producto.model.InventarioOperacion;
import com.libreria.producto.model.Producto;
import com.libreria.producto.model.TipoInventarioOperacion;
import com.libreria.producto.repository.InventarioOperacionRepository;
import com.libreria.producto.repository.ProductoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InventarioIdempotenteService {

    private final ProductoRepository productoRepository;
    private final InventarioOperacionRepository operacionRepository;

    public InventarioIdempotenteService(
            ProductoRepository productoRepository,
            InventarioOperacionRepository operacionRepository) {
        this.productoRepository = productoRepository;
        this.operacionRepository = operacionRepository;
    }

    @Transactional
    public Producto descontar(Long productoId, int cantidad, String operationKey) {

        if (operationKey == null || operationKey.isBlank()
                || operationKey.length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La clave de operación es obligatoria y debe tener máximo 150 caracteres"
            );
        }

        if (cantidad <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad debe ser mayor que cero"
            );
        }

        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Producto no encontrado"
                ));

        InventarioOperacion existente = operacionRepository
                .findByOperationKey(operationKey)
                .orElse(null);

        if (existente != null) {
            if (!productoId.equals(existente.getProductoId())
                    || cantidad != existente.getCantidad()
                    || existente.getTipo() != TipoInventarioOperacion.DESCUENTO) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave de operación pertenece a otro movimiento"
                );
            }

            // Verificar si el descuento ya fue compensado.
            if (operacionRepository.existsByTipoAndOperacionOrigenKey(
                    TipoInventarioOperacion.REPOSICION,
                    operationKey)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "El descuento fue compensado y no puede reutilizarse"
                );
            }

// El descuento ya fue confirmado anteriormente.
            return producto;
        }

        if (!Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El producto está inactivo"
            );
        }

        if (producto.getStock() < cantidad) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Stock insuficiente para " + producto.getNombre()
            );
        }

        producto.setStock(producto.getStock() - cantidad);

        InventarioOperacion operacion = new InventarioOperacion();
        operacion.setOperationKey(operationKey);
        operacion.setProductoId(productoId);
        operacion.setCantidad(cantidad);
        operacion.setTipo(TipoInventarioOperacion.DESCUENTO);

        operacionRepository.saveAndFlush(operacion);

        return productoRepository.save(producto);
    }
    @Transactional
    public Producto reponer(
            Long productoId,
            int cantidad,
            String operationKey,
            String operacionOrigenKey) {

        if (operationKey == null || operationKey.isBlank()
                || operationKey.length() > 150
                || operacionOrigenKey == null || operacionOrigenKey.isBlank()
                || operacionOrigenKey.length() > 150) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Las claves de operación y origen son obligatorias"
            );
        }

        if (cantidad <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cantidad debe ser mayor que cero"
            );
        }

        // El bloqueo serializa movimientos sobre el mismo producto.
        Producto producto = productoRepository.findByIdForUpdate(productoId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Producto no encontrado"
                ));

        InventarioOperacion existente = operacionRepository
                .findByOperationKey(operationKey)
                .orElse(null);

        if (existente != null) {
            if (!productoId.equals(existente.getProductoId())
                    || cantidad != existente.getCantidad()
                    || existente.getTipo() != TipoInventarioOperacion.REPOSICION
                    || !operacionOrigenKey.equals(existente.getOperacionOrigenKey())) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La clave corresponde a otro movimiento"
                );
            }
            return producto;
        }

        InventarioOperacion descuentoOriginal = operacionRepository
                .findByOperationKey(operacionOrigenKey)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "No existe un descuento confirmado con la clave indicada"
                ));

        if (descuentoOriginal.getTipo() != TipoInventarioOperacion.DESCUENTO
                || !productoId.equals(descuentoOriginal.getProductoId())
                || cantidad != descuentoOriginal.getCantidad()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El descuento original no coincide con la reposición"
            );
        }

        if (operacionRepository.existsByTipoAndOperacionOrigenKey(
                TipoInventarioOperacion.REPOSICION, operacionOrigenKey)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El descuento ya fue compensado"
            );
        }

        if ((long) producto.getStock() + cantidad > Integer.MAX_VALUE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La reposición supera el stock máximo permitido"
            );
        }

        producto.setStock(producto.getStock() + cantidad);

        InventarioOperacion reposicion = new InventarioOperacion();
        reposicion.setOperationKey(operationKey);
        reposicion.setProductoId(productoId);
        reposicion.setCantidad(cantidad);
        reposicion.setTipo(TipoInventarioOperacion.REPOSICION);
        reposicion.setOperacionOrigenKey(operacionOrigenKey);

        operacionRepository.saveAndFlush(reposicion);

        return productoRepository.save(producto);
    }
}