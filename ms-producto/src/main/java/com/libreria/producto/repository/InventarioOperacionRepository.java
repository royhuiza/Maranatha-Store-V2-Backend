package com.libreria.producto.repository;

import com.libreria.producto.model.InventarioOperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import com.libreria.producto.model.TipoInventarioOperacion;


import java.util.Optional;

public interface InventarioOperacionRepository
        extends JpaRepository<InventarioOperacion, Long> {

    Optional<InventarioOperacion> findByOperationKey(String operationKey);
    Optional<InventarioOperacion> findByOperacionOrigenKey(String operacionOrigenKey);
    Optional<InventarioOperacion> findByTipoAndOperacionOrigenKey(
            TipoInventarioOperacion tipo,
            String operacionOrigenKey
    );

    boolean existsByTipoAndOperacionOrigenKey(
            TipoInventarioOperacion tipo,
            String operacionOrigenKey
    );
}