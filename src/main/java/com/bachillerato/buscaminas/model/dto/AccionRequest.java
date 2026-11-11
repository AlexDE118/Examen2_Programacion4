package com.bachillerato.buscaminas.model.dto;

import com.bachillerato.buscaminas.model.juego.TipoAccion;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de POST /api/partidas/{id}/acciones. Ej.: {"tipo":"REVELAR","fila":3,"columna":4} */
public record AccionRequest(
        @NotNull(message = "El tipo de acción es obligatorio (REVELAR o BANDERA)")
        TipoAccion tipo,

        @NotNull(message = "La fila es obligatoria")
        @Min(value = 0, message = "La fila no puede ser negativa")
        @Max(value = 99, message = "Fila fuera de rango")
        Integer fila,

        @NotNull(message = "La columna es obligatoria")
        @Min(value = 0, message = "La columna no puede ser negativa")
        @Max(value = 99, message = "Columna fuera de rango")
        Integer columna
) {
}
