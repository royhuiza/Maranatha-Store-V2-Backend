
package com.libreria.ventas.service;

import com.libreria.ventas.client.FacturacionClient;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class ComprobanteFirmaLocalService {

    private final JdbcTemplate jdbcTemplate;

    public ComprobanteFirmaLocalService(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existe(Long ventaId) {

        Boolean resultado = jdbcTemplate.queryForObject(
                """
                SELECT EXISTS (
                    SELECT 1
                    FROM comprobante_firma_local
                    WHERE venta_id = ?
                )
                """,
                Boolean.class,
                ventaId
        );

        return Boolean.TRUE.equals(resultado);
    }

    public void guardar(
            Long ventaId,
            String serie,
            String numero,
            BigDecimal total,
            BigDecimal igv,
            FacturacionClient.ResultadoFirma firma
    ) {

        if (firma == null
                || !"LOCAL".equals(firma.ambiente())
                || !"FIRMADO_LOCAL".equals(firma.estado())
                || firma.validezFiscal()
                || !serie.equals(firma.serie())
                || !numero.equals(firma.numero())
                || firma.xmlFirmado() == null
                || firma.xmlFirmado().isBlank()
                || firma.contenidoQr() == null
                || firma.contenidoQr().isBlank()
                || firma.hash() == null
                || firma.hash().isBlank()
                || total.compareTo(new BigDecimal(firma.total())) != 0
                || igv.compareTo(new BigDecimal(firma.igv())) != 0) {

            throw new IllegalArgumentException(
                    "La evidencia de firma local no coincide con la venta"
            );
        }

        jdbcTemplate.update(
                """
                INSERT INTO comprobante_firma_local (
                    venta_id,
                    serie,
                    numero,
                    xml_firmado,
                    contenido_qr,
                    hash
                )
                VALUES (?, ?, ?, ?, ?, ?)
                ON CONFLICT (venta_id) DO NOTHING
                """,
                ventaId,
                serie,
                numero,
                firma.xmlFirmado(),
                firma.contenidoQr(),
                firma.hash()
        );
    }

    public Optional<String> obtenerQr(Long ventaId) {

        return jdbcTemplate.query(
                """
                SELECT contenido_qr
                FROM comprobante_firma_local
                WHERE venta_id = ?
                """,
                (rs, rowNum) -> rs.getString("contenido_qr"),
                ventaId
        ).stream().findFirst();
    }

    public Optional<String> obtenerXml(Long ventaId) {

        return jdbcTemplate.query(
                """
                SELECT xml_firmado
                FROM comprobante_firma_local
                WHERE venta_id = ?
                """,
                (rs, rowNum) -> rs.getString("xml_firmado"),
                ventaId
        ).stream().findFirst();
    }
}
