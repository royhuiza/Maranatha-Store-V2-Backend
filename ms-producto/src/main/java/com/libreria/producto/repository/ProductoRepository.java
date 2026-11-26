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

    List<Producto> findByNombreContainingIgnoreCaseOrDescripcionContainingIgnoreCase(
            String nombre,
            String descripcion
    );

    boolean existsByNombreIgnoreCase(String nombre);

    boolean existsByNombreIgnoreCaseAndIdNot(String nombre, Long id);

    List<Producto> findByActivoTrueAndStockLessThanEqualOrderByStockAsc(
            int stock
    );

    // Productos activos con stock bajo o agotado.
    @Query("select p from Producto p where p.activo = true " +
            "and p.stock <= 10 order by p.stock asc, p.nombre asc")
    List<Producto> findStockBajo();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Producto p where p.id = :id")
    Optional<Producto> findByIdForUpdate(@Param("id") Long id);
}