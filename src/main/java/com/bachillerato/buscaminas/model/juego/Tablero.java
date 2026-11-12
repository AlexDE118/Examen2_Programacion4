package com.bachillerato.buscaminas.model.juego;

/**
 * Estado en memoria de un tablero de Buscaminas. Es una clase de datos sin
 * dependencias de Spring ni de Jackson; las reglas se aplican en
 * {@code MotorBuscaminas} (capa de servicio).
 */
public class Tablero {

    private final int filas;
    private final int columnas;
    private final boolean[][] minas;
    private final boolean[][] reveladas;
    private final boolean[][] banderas;
    /** Casilla con mina que el jugador pisó (null si no ha perdido). */
    private int[] explotada;

    public Tablero(int filas, int columnas) {
        if (filas <= 0 || columnas <= 0) {
            throw new IllegalArgumentException("Dimensiones inválidas");
        }
        this.filas = filas;
        this.columnas = columnas;
        this.minas = new boolean[filas][columnas];
        this.reveladas = new boolean[filas][columnas];
        this.banderas = new boolean[filas][columnas];
    }

    public int getFilas() { return filas; }
    public int getColumnas() { return columnas; }

    public boolean dentro(int f, int c) {
        return f >= 0 && f < filas && c >= 0 && c < columnas;
    }

    public boolean tieneMina(int f, int c) { return minas[f][c]; }
    public boolean estaRevelada(int f, int c) { return reveladas[f][c]; }
    public boolean tieneBandera(int f, int c) { return banderas[f][c]; }

    public void ponerMina(int f, int c) { minas[f][c] = true; }
    public void revelar(int f, int c) { reveladas[f][c] = true; }
    public void setBandera(int f, int c, boolean valor) { banderas[f][c] = valor; }

    public int[] getExplotada() { return explotada; }
    public void setExplotada(int f, int c) { this.explotada = new int[]{f, c}; }

    public boolean esExplotada(int f, int c) {
        return explotada != null && explotada[0] == f && explotada[1] == c;
    }

    public int contarMinas() { return contar(minas); }
    public int contarReveladas() { return contar(reveladas); }
    public int contarBanderas() { return contar(banderas); }

    /** Casillas que hay que descubrir para ganar. */
    public int casillasSeguras() {
        return filas * columnas - contarMinas();
    }

    private int contar(boolean[][] matriz) {
        int total = 0;
        for (boolean[] fila : matriz) {
            for (boolean v : fila) {
                if (v) {
                    total++;
                }
            }
        }
        return total;
    }
}
