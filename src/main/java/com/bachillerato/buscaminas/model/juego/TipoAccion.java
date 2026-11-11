package com.bachillerato.buscaminas.model.juego;

/** Acciones que el jugador puede enviar sobre una casilla. */
public enum TipoAccion {
    /** Clic izquierdo: descubrir la casilla. */
    REVELAR,
    /** Clic derecho: poner o quitar una bandera. */
    BANDERA
}
