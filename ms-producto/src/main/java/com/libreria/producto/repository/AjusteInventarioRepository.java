package com.libreria.producto.repository;

import com.libreria.producto.model.AjusteInventario;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AjusteInventarioRepository extends JpaRepository<AjusteInventario, Long> {
    List<AjusteInventario> findByProductoIdOrderByIdDesc(Long productoId);
}
