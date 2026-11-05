
package com.libreria.facturacion.service;

import com.libreria.facturacion.dto.ConsultaRucResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ConsultaRucServiceImpl implements ConsultaRucService {

    private final boolean consultaHabilitada;

    public ConsultaRucServiceImpl(
            @Value("${consulta-ruc.enabled:false}")
            boolean consultaHabilitada
    ) {
        this.consultaHabilitada = consultaHabilitada;
    }

    @Override
    public ConsultaRucResponse consultarPorRuc(String ruc) {

        if (ruc == null || !ruc.matches("\\d{11}")) {
            throw new IllegalArgumentException(
                    "El RUC debe contener exactamente 11 digitos"
            );
        }

        if (!consultaHabilitada) {
            throw new IllegalStateException(
                    "La consulta RUC no esta configurada"
            );
        }

        throw new UnsupportedOperationException(
                "Todavia no se ha implementado el proveedor de consulta RUC"
        );
    }
}
