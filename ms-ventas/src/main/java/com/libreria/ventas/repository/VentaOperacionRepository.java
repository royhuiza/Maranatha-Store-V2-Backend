package com.libreria.ventas.repository;

import com.libreria.ventas.model.VentaOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VentaOperacionRepository
        extends JpaRepository<VentaOperacion, Long> {

    Optional<VentaOperacion> findByIdempotencyKey(String idempotencyKey);
}