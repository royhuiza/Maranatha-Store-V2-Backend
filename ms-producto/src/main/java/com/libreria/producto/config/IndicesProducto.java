package com.libreria.producto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * T12: índices para que listar y buscar productos por nombre no recorra toda la tabla.
 * Las búsquedas usan upper(nombre), por eso los índices se crean sobre esa expresión
 * (JPA @Index no admite índices de expresión). "if not exists" los hace seguros al reiniciar.
 */
@Component
public class IndicesProducto implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndicesProducto.class);
    private final JdbcTemplate jdbc;

    public IndicesProducto(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Búsqueda exacta sin mayúsculas: validación de nombre repetido (T06).
        ejecutar("create index if not exists idx_productos_nombre on productos (upper(nombre))");
        // Búsqueda por parte del nombre (LIKE '%texto%') con trigramas de PostgreSQL.
        if (ejecutar("create extension if not exists pg_trgm")) {
            ejecutar("create index if not exists idx_productos_nombre_trgm on productos using gin (upper(nombre) gin_trgm_ops)");
        }
        // Alertas de stock bajo (T39): filtra por activo y stock.
        ejecutar("create index if not exists idx_productos_activo_stock on productos (activo, stock)");
    }

    private boolean ejecutar(String sql) {
        try {
            jdbc.execute(sql);
            return true;
        } catch (RuntimeException ex) {
            log.warn("No se pudo crear el índice ({}): {}", sql, ex.getMessage());
            return false;
        }
    }
}
