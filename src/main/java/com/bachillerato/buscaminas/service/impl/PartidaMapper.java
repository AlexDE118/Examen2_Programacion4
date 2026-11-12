package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.model.dto.PartidaDTO;
import com.bachillerato.buscaminas.model.dto.PartidaResumenDTO;
import com.bachillerato.buscaminas.model.entity.EstadoPartida;
import com.bachillerato.buscaminas.model.entity.Partida;
import com.bachillerato.buscaminas.model.juego.ResultadoPartida;
import com.bachillerato.buscaminas.model.juego.Tablero;
import com.bachillerato.buscaminas.model.juego.TableroJson;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Construye los DTO de salida. Aquí se aplica la «regla de oro»: mientras la
 * partida está EN_CURSO, las celdas solo pueden ser "O", "B" o un número;
 * la posición de las minas no sale nunca del servidor.
 */
@Component
public class PartidaMapper {

    private final MotorBuscaminas motor;

    public PartidaMapper(MotorBuscaminas motor) {
        this.motor = motor;
    }

    public PartidaDTO aDto(Partida p, Tablero t, TableroJson doc, long ahoraMs) {
        ResultadoPartida resultado = resultadoDe(p);
        boolean enCurso = p.getEstado() == EstadoPartida.EN_CURSO;

        List<List<String>> celdas = new ArrayList<>(t.getFilas());
        for (int f = 0; f < t.getFilas(); f++) {
            List<String> fila = new ArrayList<>(t.getColumnas());
            for (int c = 0; c < t.getColumnas(); c++) {
                fila.add(enCurso ? celdaEnCurso(t, f, c) : celdaFinal(t, f, c, resultado));
            }
            celdas.add(fila);
        }

        long transcurrido = enCurso ? transcurridoEnCurso(p, doc, ahoraMs) : duracionMs(p, doc);
        return new PartidaDTO(
                p.getId(),
                p.getNivel().name(),
                p.getEstado().name(),
                resultado.name(),
                t.getFilas(),
                t.getColumnas(),
                p.getNivel().getMinas(),
                t.contarBanderas(),
                p.getMovimientos(),
                p.getEstrellas(),
                p.getTiempoMs() == null ? null : p.getTiempoMs().longValue(),
                transcurrido,
                p.getFechaInicio(),
                p.getFechaFin(),
                celdas);
    }

    public PartidaResumenDTO aResumen(Partida p, TableroJson doc) {
        Long duracion = p.getEstado() == EstadoPartida.EN_CURSO ? null : duracionMs(p, doc);
        return new PartidaResumenDTO(
                p.getId(),
                p.getNivel().name(),
                p.getEstado().name(),
                resultadoDe(p).name(),
                p.getFechaInicio(),
                p.getFechaFin(),
                duracion,
                p.getTiempoMs() == null ? null : p.getTiempoMs().longValue(),
                p.getMovimientos(),
                p.getEstrellas());
    }

    /** TERMINADA con estrellas es victoria; TERMINADA sin estrellas es derrota. */
    public static ResultadoPartida resultadoDe(Partida p) {
        return switch (p.getEstado()) {
            case EN_CURSO -> ResultadoPartida.EN_CURSO;
            case ABANDONADA -> ResultadoPartida.ABANDONADA;
            case TERMINADA -> p.getEstrellas() != null && p.getEstrellas() > 0
                    ? ResultadoPartida.VICTORIA : ResultadoPartida.DERROTA;
        };
    }

    private String celdaEnCurso(Tablero t, int f, int c) {
        if (t.estaRevelada(f, c) && !t.tieneMina(f, c)) {
            return String.valueOf(motor.minasAdyacentes(t, f, c));
        }
        return t.tieneBandera(f, c) ? "B" : "O";
    }

    private String celdaFinal(Tablero t, int f, int c, ResultadoPartida resultado) {
        boolean victoria = resultado == ResultadoPartida.VICTORIA;
        if (t.tieneMina(f, c)) {
            if (t.esExplotada(f, c)) {
                return "X";
            }
            return victoria || t.tieneBandera(f, c) ? "B" : "M";
        }
        if (t.estaRevelada(f, c)) {
            return String.valueOf(motor.minasAdyacentes(t, f, c));
        }
        if (t.tieneBandera(f, c)) {
            return victoria ? "B" : "BI";
        }
        return "O";
    }

    private long transcurridoEnCurso(Partida p, TableroJson doc, long ahoraMs) {
        long inicio = doc != null && doc.inicioMs != null ? doc.inicioMs : epochMs(p);
        return Math.max(0, ahoraMs - inicio);
    }

    private long duracionMs(Partida p, TableroJson doc) {
        if (doc != null && doc.duracionMs != null) {
            return doc.duracionMs;
        }
        if (p.getTiempoMs() != null) {
            return p.getTiempoMs();
        }
        if (p.getFechaInicio() != null && p.getFechaFin() != null) {
            return Math.max(0, Duration.between(p.getFechaInicio(), p.getFechaFin()).toMillis());
        }
        return 0;
    }

    private static long epochMs(Partida p) {
        return p.getFechaInicio().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
