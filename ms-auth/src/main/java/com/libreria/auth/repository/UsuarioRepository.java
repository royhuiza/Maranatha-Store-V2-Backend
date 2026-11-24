package com.libreria.auth.repository;

import com.libreria.auth.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from Usuario u order by u.id")
    java.util.List<Usuario> bloquearUsuarios();
    long countByRolAndActivoTrue(com.libreria.auth.model.Rol rol);
    Optional<Usuario> findOptionalByEmailIgnoreCase(String email);
    Usuario findByUsername(String username);
    Optional<Usuario> findOptionalByUsernameIgnoreCase(String username);
    boolean existsByUsernameIgnoreCase(String username);
    boolean existsByEmailIgnoreCase(String email);
}
