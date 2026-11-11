package com.bachillerato.buscaminas.model.dto;

import com.bachillerato.buscaminas.model.juego.Nivel;
import jakarta.validation.constraints.NotNull;

/** Cuerpo de POST /api/partidas. Ej.: {"nivel":"PRINCIPIANTE"} */
public record CrearPartidaRequest(
        @NotNull(message = "El nivel es obligatorio (PRINCIPIANTE, INTERMEDIO o EXPERTO)")
        Nivel nivel
) {
}
