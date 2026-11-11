package com.bachillerato.buscaminas.model.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Estado FILTRADO de una partida. «celdas» es una matriz [fila][columna] de códigos:
 * <ul>
 *   <li>"O"  casilla oculta</li>
 *   <li>"B"  bandera</li>
 *   <li>"0".."8" casilla abierta con su número de minas adyacentes</li>
 *   <li>"M"  mina (solo cuando la partida terminó)</li>
 *   <li>"X"  mina que explotó (solo en derrota)</li>
 *   <li>"BI" bandera mal puesta (solo en derrota)</li>
 * </ul>
 * Mientras la partida está EN_CURSO nunca aparecen "M", "X" ni "BI".
 */
public record PartidaDTO(
        Long id,
        String nivel,
        String estado,
        String resultado,
        int filas,
        int columnas,
        int totalMinas,
        int banderas,
        int movimientos,
        int estrellas,
        Long tiempoMs,
        long transcurridoMs,
        LocalDateTime fechaInicio,
        LocalDateTime fechaFin,
        List<List<String>> celdas
) {
}
