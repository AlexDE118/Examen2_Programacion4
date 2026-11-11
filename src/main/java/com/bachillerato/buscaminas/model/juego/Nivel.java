package com.bachillerato.buscaminas.model.juego;

/**
 * Niveles del juego definidos como constantes del backend (no hay tabla de catálogo).
 * Los umbrales de tiempo deciden las estrellas de una victoria (1 a 3).
 */
public enum Nivel {

    PRINCIPIANTE("Principiante", 9, 9, 10, 60_000L, 120_000L),
    INTERMEDIO("Intermedio", 16, 16, 40, 240_000L, 480_000L),
    EXPERTO("Experto", 16, 30, 99, 600_000L, 1_200_000L);

    private final String nombre;
    private final int filas;
    private final int columnas;
    private final int minas;
    private final long msTresEstrellas;
    private final long msDosEstrellas;

    Nivel(String nombre, int filas, int columnas, int minas, long msTresEstrellas, long msDosEstrellas) {
        this.nombre = nombre;
        this.filas = filas;
        this.columnas = columnas;
        this.minas = minas;
        this.msTresEstrellas = msTresEstrellas;
        this.msDosEstrellas = msDosEstrellas;
    }

    public String getNombre() { return nombre; }
    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }
    public int getMinas() { return minas; }
    public long getMsTresEstrellas() { return msTresEstrellas; }
    public long getMsDosEstrellas() { return msDosEstrellas; }

    /** Estrellas de una victoria según su tiempo. Una derrota siempre vale 0. */
    public int estrellasPara(long tiempoMs) {
        if (tiempoMs <= msTresEstrellas) {
            return 3;
        }
        if (tiempoMs <= msDosEstrellas) {
            return 2;
        }
        return 1;
    }
}
