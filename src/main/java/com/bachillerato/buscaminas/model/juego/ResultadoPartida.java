package com.bachillerato.buscaminas.model.juego;

/**
 * Resultado visible para el jugador. Se deriva de las columnas estado y estrellas:
 * TERMINADA con estrellas &gt;= 1 es VICTORIA; TERMINADA con 0 estrellas es DERROTA.
 */
public enum ResultadoPartida {
    EN_CURSO,
    VICTORIA,
    DERROTA,
    ABANDONADA
}
