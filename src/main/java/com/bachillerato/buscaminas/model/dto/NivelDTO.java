package com.bachillerato.buscaminas.model.dto;

import com.bachillerato.buscaminas.model.juego.Nivel;

public record NivelDTO(
        String codigo,
        String nombre,
        int filas,
        int columnas,
        int minas,
        long msTresEstrellas,
        long msDosEstrellas
) {
    public static NivelDTO de(Nivel n) {
        return new NivelDTO(n.name(), n.getNombre(), n.getFilas(), n.getColumnas(), n.getMinas(),
                n.getMsTresEstrellas(), n.getMsDosEstrellas());
    }
}
