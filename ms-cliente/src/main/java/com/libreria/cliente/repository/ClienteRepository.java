package com.libreria.cliente.repository;

import com.libreria.cliente.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByEmailIgnoreCase(String email);
    boolean existsByNumeroDocumento(String numeroDocumento);
    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);
    boolean existsByNumeroDocumentoAndIdNot(String numeroDocumento, Long id);
}
