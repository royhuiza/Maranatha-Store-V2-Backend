package com.libreria.producto.repository;

import com.libreria.producto.model.Producto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
    List<Producto> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(String nombre, String descripcion);

    boolean existsByNombreIgnoreCase(String nombre);

    /** T39: artículos activos cuyo stock llegó a su stock mínimo (incluye los que no tienen stock). */
    @Query("select p from Producto p where p.activo = true and p.stock <= p.stockMinimo order by p.stock asc, p.nombre asc")
    List<Producto> findStockBajo();

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}
