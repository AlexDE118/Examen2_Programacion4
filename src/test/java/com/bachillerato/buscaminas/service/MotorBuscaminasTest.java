package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.exception.ApiException;
import com.bachillerato.buscaminas.model.juego.Nivel;
import com.bachillerato.buscaminas.model.juego.ResultadoMovimiento;
import com.bachillerato.buscaminas.model.juego.Tablero;
import com.bachillerato.buscaminas.service.impl.MotorBuscaminas;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reglas del juego de la capa de servicio, con tableros construidos a mano. */
class MotorBuscaminasTest {

    private final MotorBuscaminas motor = new MotorBuscaminas(new Random(42));

    /** Tablero 9x9 con una pared de minas en la columna 4. */
    private Tablero tableroConPared() {
        Tablero t = new Tablero(9, 9);
        for (int f = 0; f < 9; f++) {
            t.ponerMina(f, 4);
        }
        return t;
    }

    @Test
    void crearTableroColocaLaCantidadExactaDeMinas() {
        for (Nivel n : Nivel.values()) {
            Tablero t = motor.crearTablero(n);
            assertEquals(n.getFilas(), t.getFilas());
            assertEquals(n.getColumnas(), t.getColumnas());
            assertEquals(n.getMinas(), t.contarMinas());
            assertEquals(0, t.contarReveladas());
        }
    }

    @Test
    void calculaMinasAdyacentes() {
        Tablero t = tableroConPared();
        assertEquals(0, motor.minasAdyacentes(t, 0, 0));
        assertEquals(2, motor.minasAdyacentes(t, 0, 3)); // esquina: (0,4) y (1,4)
        assertEquals(3, motor.minasAdyacentes(t, 4, 3)); // (3,4) (4,4) (5,4)
        assertEquals(-1, motor.calcularAdyacentes(t)[2][4]);
    }

    @Test
    void aperturaEnCadenaAbreLaZonaVaciaYSuBordeDeNumeros() {
        Tablero t = tableroConPared();

        ResultadoMovimiento r = motor.revelar(t, 0, 0);

        assertEquals(ResultadoMovimiento.CONTINUA, r);
        // Se abren las columnas 0..3 completas (36 casillas) y nada más.
        assertEquals(36, t.contarReveladas());
        for (int f = 0; f < 9; f++) {
            for (int c = 0; c < 4; c++) {
                assertTrue(t.estaRevelada(f, c), "debería estar abierta (" + f + "," + c + ")");
            }
            for (int c = 4; c < 9; c++) {
                assertFalse(t.estaRevelada(f, c), "no debería abrirse (" + f + "," + c + ")");
            }
        }
    }

    @Test
    void aperturaEnCadenaRespetaLasBanderas() {
        Tablero t = tableroConPared();
        motor.alternarBandera(t, 8, 0);

        motor.revelar(t, 0, 0);

        assertFalse(t.estaRevelada(8, 0));
        assertEquals(35, t.contarReveladas());
    }

    @Test
    void revelarUnNumeroNoAbreEnCadena() {
        Tablero t = tableroConPared();
        motor.revelar(t, 4, 3);
        assertEquals(1, t.contarReveladas());
    }

    @Test
    void revelarMinaEsDerrota() {
        Tablero t = tableroConPared();
        assertEquals(ResultadoMovimiento.MINA, motor.revelar(t, 3, 4));
        assertTrue(t.esExplotada(3, 4));
    }

    @Test
    void descubrirTodasLasSegurasEsVictoria() {
        Tablero t = tableroConPared();
        assertEquals(ResultadoMovimiento.CONTINUA, motor.revelar(t, 0, 0));
        assertEquals(ResultadoMovimiento.VICTORIA, motor.revelar(t, 0, 8));
        assertTrue(motor.esVictoria(t));
    }

    @Test
    void accionesInvalidasLanzanError400() {
        Tablero t = tableroConPared();
        assertThrows(ApiException.class, () -> motor.revelar(t, 9, 0));
        motor.revelar(t, 4, 3);
        assertThrows(ApiException.class, () -> motor.revelar(t, 4, 3));
        assertThrows(ApiException.class, () -> motor.alternarBandera(t, 4, 3));
        motor.alternarBandera(t, 0, 0);
        assertThrows(ApiException.class, () -> motor.revelar(t, 0, 0));
    }
}
