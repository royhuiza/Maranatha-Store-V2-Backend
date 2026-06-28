package com.libreria.ventas.repository;

import com.libreria.ventas.model.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long> {
    @EntityGraph(attributePaths = "detalles")
    List<Venta> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findOneById(Long id);
}
