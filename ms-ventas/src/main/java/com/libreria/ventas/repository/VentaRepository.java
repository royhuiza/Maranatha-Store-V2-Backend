package com.libreria.ventas.repository;

import com.libreria.ventas.model.Venta;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {
    @EntityGraph(attributePaths = "detalles")
    List<Venta> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findOneById(Long id);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Venta v where v.id = :id")
    Optional<Venta> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
}
