package com.libreria.producto.dto;

import com.libreria.producto.model.TipoAfectacionIgv;
import jakarta.validation.constraints.NotNull;

public record AfectacionIgvRequest(
        @NotNull(message = "La clasificacion tributaria es obligatoria")
        TipoAfectacionIgv afectacionIgv
) {}
