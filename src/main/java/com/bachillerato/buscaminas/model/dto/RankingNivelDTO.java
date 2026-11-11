package com.bachillerato.buscaminas.model.dto;

import java.util.List;

/** Tabla de posiciones de un nivel y la posición del usuario actual (null si no aparece). */
public record RankingNivelDTO(
        String nivel,
        String nombreNivel,
        List<RankingFilaDTO> posiciones,
        RankingFilaDTO miPosicion
) {
}
