package com.libreria.ventas.repository;

import com.libreria.ventas.model.Venta;
import com.libreria.ventas.model.EstadoSunat;
import com.libreria.ventas.model.EstadoVenta;
import com.libreria.ventas.model.TipoComprobante;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {
    @EntityGraph(attributePaths = "detalles")
    List<Venta> findAllByOrderByFechaDesc();

    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findOneById(Long id);

    // Carga "detalles" en la misma consulta: sin EntityGraph la coleccion
    // queda lazy y Jackson falla al serializar la venta devuelta por
    // anularVenta fuera de la transaccion (LazyInitializationException).
    @EntityGraph(attributePaths = "detalles")
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select v from Venta v where v.id = :id")
    Optional<Venta> findLockedById(@org.springframework.data.repository.query.Param("id") Long id);
    @EntityGraph(attributePaths = "detalles")
    Optional<Venta> findByIdempotencyKey(
            String idempotencyKey
    );

    @Modifying(clearAutomatically = true)
    @Query("""
    UPDATE Venta v
    SET v.estadoSunat = :nuevo
    WHERE v.id = :id
      AND v.estadoSunat = :anterior
      AND v.estado = :estadoVenta
      AND v.tipoComprobante <> :excluido
      AND v.validadoSunat = false
    """)
    int reservarEmisionFiscal(
            @Param("id") Long id,
            @Param("anterior") EstadoSunat anterior,
            @Param("nuevo") EstadoSunat nuevo,
            @Param("estadoVenta") EstadoVenta estadoVenta,
            @Param("excluido") TipoComprobante excluido
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Venta v
        SET v.estadoSunat = :nuevo,
            v.codigoRespuestaSunat = :codigo,
            v.mensajeRespuestaSunat = :mensaje,
            v.fechaRespuestaSunat = :fecha
        WHERE v.id = :id
          AND v.estadoSunat = :anterior
        """)
    int actualizarResultadoFiscal(
            @Param("id") Long id,
            @Param("anterior") EstadoSunat anterior,
            @Param("nuevo") EstadoSunat nuevo,
            @Param("codigo") String codigo,
            @Param("mensaje") String mensaje,
            @Param("fecha") LocalDateTime fecha
    );
}
