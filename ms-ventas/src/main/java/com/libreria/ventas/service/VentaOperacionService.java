package com.libreria.ventas.service;

import com.libreria.ventas.model.EstadoVentaOperacion;
import com.libreria.ventas.model.VentaOperacion;
import com.libreria.ventas.repository.VentaOperacionRepository;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;


import java.util.Optional;

@Service
public class VentaOperacionService {

    private final VentaOperacionRepository repository;
    private final VentaOperacionInsercionService insercionService;

    public VentaOperacionService(
            VentaOperacionRepository repository,
            VentaOperacionInsercionService insercionService) {

        this.repository = repository;
        this.insercionService = insercionService;
    }
    public ResultadoReservaVenta reservarConResultado(
            String idempotencyKey,
            String requestHash) {

        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Clave de idempotencia inválida"
            );
        }

        if (requestHash == null || !requestHash.matches("[0-9a-f]{64}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Huella de solicitud inválida"
            );
        }

        VentaOperacion existente = repository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existente != null) {
            return new ResultadoReservaVenta(
                    validarOperacion(existente, requestHash),
                    false
            );
        }

        try {
            VentaOperacion creada = insercionService.insertar(
                    idempotencyKey,
                    requestHash
            );

            return new ResultadoReservaVenta(creada, true);

        } catch (DataIntegrityViolationException ex) {

            VentaOperacion ganadora = repository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "La operación está siendo registrada; reintente la consulta"
                    ));

            return new ResultadoReservaVenta(
                    validarOperacion(ganadora, requestHash),
                    false
            );
        }
    }

    public VentaOperacion reservar(String idempotencyKey, String requestHash) {

        if (idempotencyKey == null || idempotencyKey.isBlank()
                || idempotencyKey.length() > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Clave de idempotencia inválida"
            );
        }

        if (requestHash == null || !requestHash.matches("[0-9a-f]{64}")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Huella de solicitud inválida"
            );
        }

        VentaOperacion existente = repository
                .findByIdempotencyKey(idempotencyKey)
                .orElse(null);

        if (existente != null) {
            return validarOperacion(existente, requestHash);
        }

        try {
            return insercionService.insertar(idempotencyKey, requestHash);

        } catch (DataIntegrityViolationException ex) {

            VentaOperacion ganadora = repository
                    .findByIdempotencyKey(idempotencyKey)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "La operación está siendo registrada; reintente la consulta"
                    ));

            return validarOperacion(ganadora, requestHash);
        }
    }

    private VentaOperacion validarOperacion(
            VentaOperacion operacion,
            String requestHash) {

        if (!requestHash.equals(operacion.getRequestHash())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La clave de idempotencia pertenece a otra compra"
            );
        }

        return operacion;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completar(Long operacionId, Long ventaId) {

        VentaOperacion operacion = repository.findById(operacionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Operación de venta no encontrada"
                ));

        operacion.setVentaId(ventaId);
        operacion.setEstado(EstadoVentaOperacion.COMPLETADA);

        repository.save(operacion);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void marcarRevision(Long operacionId) {

        VentaOperacion operacion = repository.findById(operacionId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Operación de venta no encontrada"
                ));

        if (operacion.getEstado() != EstadoVentaOperacion.COMPLETADA) {
            operacion.setEstado(EstadoVentaOperacion.REQUIERE_REVISION);
            repository.save(operacion);
        }
    }
}