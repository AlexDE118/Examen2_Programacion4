package com.bachillerato.buscaminas.model.dto;

import java.time.LocalDateTime;

/** Fila del historial de un jugador (sin el tablero). */
public record PartidaResumenDTO(
        Long id,
        String nivel,
        String estado,
        String resultado,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        Long duracionMs,
        Long tiempoMs,
        int movimientos,
        int estrellas
) {
}
