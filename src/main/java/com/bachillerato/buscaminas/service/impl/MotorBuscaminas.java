package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.exception.ApiException;
import com.bachillerato.buscaminas.model.juego.Nivel;
import com.bachillerato.buscaminas.model.juego.ResultadoMovimiento;
import com.bachillerato.buscaminas.model.juego.Tablero;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Random;

/**
 * Reglas del Buscaminas. Todo se resuelve aquí, en el servidor:
 * colocación aleatoria de minas, conteo de adyacentes, apertura en cadena
 * y detección de victoria o derrota.
 */
@Component
public class MotorBuscaminas {

    /** Desplazamientos de las 8 casillas vecinas. */
    private static final int[][] VECINOS = {
            {-1, -1}, {-1, 0}, {-1, 1},
            {0, -1},           {0, 1},
            {1, -1},  {1, 0},  {1, 1}
    };

    private final Random random;

    public MotorBuscaminas() {
        this(new SecureRandom());
    }

    /** Constructor para pruebas, con una semilla controlada. */
    public MotorBuscaminas(Random random) {
        this.random = random;
    }

    /**
     * Crea un tablero del nivel indicado y coloca las minas al azar.
     * Se usa un barajado parcial de Fisher-Yates sobre los índices de las
     * casillas, así nunca se repite una posición.
     */
    public Tablero crearTablero(Nivel nivel) {
        Tablero tablero = new Tablero(nivel.getFilas(), nivel.getColumnas());
        int total = nivel.getFilas() * nivel.getColumnas();
        int[] indices = new int[total];
        for (int i = 0; i < total; i++) {
            indices[i] = i;
        }
        for (int i = 0; i < nivel.getMinas(); i++) {
            int j = i + random.nextInt(total - i);
            int tmp = indices[i];
            indices[i] = indices[j];
            indices[j] = tmp;
            tablero.ponerMina(indices[i] / nivel.getColumnas(), indices[i] % nivel.getColumnas());
        }
        return tablero;
    }

    /** Número de minas en las 8 casillas vecinas (0 a 8). */
    public int minasAdyacentes(Tablero t, int f, int c) {
        int cuenta = 0;
        for (int[] d : VECINOS) {
            int nf = f + d[0];
            int nc = c + d[1];
            if (t.dentro(nf, nc) && t.tieneMina(nf, nc)) {
                cuenta++;
            }
        }
        return cuenta;
    }

    /** Matriz completa de adyacentes; para una casilla con mina vale -1. */
    public int[][] calcularAdyacentes(Tablero t) {
        int[][] res = new int[t.getFilas()][t.getColumnas()];
        for (int f = 0; f < t.getFilas(); f++) {
            for (int c = 0; c < t.getColumnas(); c++) {
                res[f][c] = t.tieneMina(f, c) ? -1 : minasAdyacentes(t, f, c);
            }
        }
        return res;
    }

    /**
     * Descubre una casilla. Si tiene mina, la partida se pierde. Si su número es 0,
     * abre en cadena (BFS) todas las casillas vacías contiguas y su borde de números.
     * Las casillas con bandera no se abren en la cadena.
     */
    public ResultadoMovimiento revelar(Tablero t, int f, int c) {
        validarCoordenadas(t, f, c);
        if (t.estaRevelada(f, c)) {
            throw ApiException.solicitudInvalida("La casilla ya está descubierta");
        }
        if (t.tieneBandera(f, c)) {
            throw ApiException.solicitudInvalida("Quite la bandera antes de descubrir la casilla");
        }
        if (t.tieneMina(f, c)) {
            t.revelar(f, c);
            t.setExplotada(f, c);
            return ResultadoMovimiento.MINA;
        }

        aperturaEnCadena(t, f, c);
        return esVictoria(t) ? ResultadoMovimiento.VICTORIA : ResultadoMovimiento.CONTINUA;
    }

    /** Recorrido en anchura desde (f, c). Devuelve cuántas casillas se abrieron. */
    public int aperturaEnCadena(Tablero t, int f, int c) {
        Deque<int[]> cola = new ArrayDeque<>();
        t.revelar(f, c);
        cola.add(new int[]{f, c});
        int abiertas = 1;

        while (!cola.isEmpty()) {
            int[] actual = cola.poll();
            if (minasAdyacentes(t, actual[0], actual[1]) != 0) {
                continue; // un número es borde: no se expande
            }
            for (int[] d : VECINOS) {
                int nf = actual[0] + d[0];
                int nc = actual[1] + d[1];
                if (t.dentro(nf, nc) && !t.estaRevelada(nf, nc)
                        && !t.tieneBandera(nf, nc) && !t.tieneMina(nf, nc)) {
                    t.revelar(nf, nc);
                    abiertas++;
                    cola.add(new int[]{nf, nc});
                }
            }
        }
        return abiertas;
    }

    /** Pone o quita una bandera en una casilla todavía oculta. */
    public void alternarBandera(Tablero t, int f, int c) {
        validarCoordenadas(t, f, c);
        if (t.estaRevelada(f, c)) {
            throw ApiException.solicitudInvalida("No se puede poner bandera en una casilla descubierta");
        }
        t.setBandera(f, c, !t.tieneBandera(f, c));
    }

    /** Victoria: todas las casillas sin mina están descubiertas. */
    public boolean esVictoria(Tablero t) {
        int seguras = 0;
        for (int f = 0; f < t.getFilas(); f++) {
            for (int c = 0; c < t.getColumnas(); c++) {
                if (!t.tieneMina(f, c)) {
                    if (!t.estaRevelada(f, c)) {
                        return false;
                    }
                    seguras++;
                }
            }
        }
        return seguras > 0;
    }

    private void validarCoordenadas(Tablero t, int f, int c) {
        if (!t.dentro(f, c)) {
            throw ApiException.solicitudInvalida("La casilla (" + f + ", " + c + ") está fuera del tablero");
        }
    }
}
