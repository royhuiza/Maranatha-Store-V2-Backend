package com.libreria.facturacion.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import com.libreria.facturacion.dto.TipoAfectacionIgv;

@Service
public class CalculoIgvService {

    private static final BigDecimal FACTOR_IGV =
            new BigDecimal("1.18");

    private static final BigDecimal TASA_IGV =
            new BigDecimal("0.18");

    public BigDecimal calcularBaseImponible(BigDecimal precioFinal) {
        validarPrecio(precioFinal);

        return precioFinal.divide(
                FACTOR_IGV,
                2,
                RoundingMode.HALF_UP
        );
    }

    public BigDecimal calcularIgv(BigDecimal precioFinal) {
        BigDecimal base = calcularBaseImponible(precioFinal);

        return precioFinal.setScale(2, RoundingMode.HALF_UP)
                .subtract(base);
    }

    public BigDecimal calcularTotal(BigDecimal base, BigDecimal igv) {
        if (base == null || igv == null) {
            throw new IllegalArgumentException(
                    "La base imponible y el IGV son obligatorios"
            );
        }

        return base.add(igv)
                .setScale(2, RoundingMode.HALF_UP);
    }

    public ResultadoIgv calcularPorAfectacion(
            BigDecimal precioFinal,
            TipoAfectacionIgv afectacionIgv
    ) {
        validarPrecio(precioFinal);

        if (afectacionIgv == null) {
            throw new IllegalArgumentException(
                    "La clasificación tributaria es obligatoria"
            );
        }

        BigDecimal total = precioFinal.setScale(
                2, RoundingMode.HALF_UP
        );

        if (afectacionIgv == TipoAfectacionIgv.GRAVADO) {
            BigDecimal base = calcularBaseImponible(total);
            BigDecimal igv = total.subtract(base);

            return new ResultadoIgv(base, igv, total);
        }

        // Operaciones exoneradas o inafectas.
        return new ResultadoIgv(
                total,
                BigDecimal.ZERO.setScale(2),
                total
        );
    }

    public record ResultadoIgv(
            BigDecimal base,
            BigDecimal igv,
            BigDecimal total
    ) {}

    private void validarPrecio(BigDecimal precio) {
        if (precio == null || precio.signum() < 0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor o igual a cero"
            );
        }
    }
}
