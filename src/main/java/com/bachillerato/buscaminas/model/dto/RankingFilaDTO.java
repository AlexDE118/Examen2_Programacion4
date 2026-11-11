package com.bachillerato.buscaminas.model.dto;

import java.time.LocalDateTime;

/** Una posición del ranking (no expone el email de los jugadores). */
public record RankingFilaDTO(
        int posicion,
        Long usuarioId,
        String nombreUsuario,
        long mejorTiempoMs,
        long partidasTerminadas,
        double promedioMovimientos,
        LocalDateTime ultimoJuego,
        boolean esUsuarioActual
) {
}
