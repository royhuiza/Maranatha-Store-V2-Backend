package com.libreria.ventas.service;

import com.libreria.ventas.model.Venta;
import com.libreria.ventas.repository.VentaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class VentaPersistenciaService {

    private final VentaRepository ventaRepository;

    public VentaPersistenciaService(VentaRepository ventaRepository) {
        this.ventaRepository = ventaRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Venta guardarVenta(Venta venta, String idempotencyKey) {

        venta.setIdempotencyKey(idempotencyKey);

        Venta guardada = ventaRepository.saveAndFlush(venta);

        guardada.setNumero(
                String.format("%08d", guardada.getId())
        );

        return ventaRepository.saveAndFlush(guardada);
    }

    @Transactional(readOnly = true)
    public Optional<Venta> buscarPorIdempotencyKey(String idempotencyKey) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Clave de idempotencia obligatoria"
            );
        }

        return ventaRepository.findByIdempotencyKey(idempotencyKey);
    }
}