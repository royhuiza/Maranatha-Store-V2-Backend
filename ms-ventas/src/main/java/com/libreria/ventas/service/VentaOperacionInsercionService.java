package com.libreria.ventas.service;

import com.libreria.ventas.model.EstadoVentaOperacion;
import com.libreria.ventas.model.VentaOperacion;
import com.libreria.ventas.repository.VentaOperacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaOperacionInsercionService {

    private final VentaOperacionRepository repository;

    public VentaOperacionInsercionService(VentaOperacionRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public VentaOperacion insertar(String idempotencyKey, String requestHash) {

        VentaOperacion operacion = new VentaOperacion();
        operacion.setIdempotencyKey(idempotencyKey);
        operacion.setRequestHash(requestHash);
        operacion.setEstado(EstadoVentaOperacion.EN_PROCESO);

        return repository.saveAndFlush(operacion);
    }
}