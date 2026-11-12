package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.model.juego.Tablero;
import com.bachillerato.buscaminas.model.juego.TableroJson;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Convierte entre el documento JSON de la columna «tablero» y el modelo en memoria. */
@Component
public class TableroMapper {

    private final ObjectMapper objectMapper;

    public TableroMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TableroJson leer(String json) {
        try {
            TableroJson doc = objectMapper.readValue(json, TableroJson.class);
            if (doc.filas <= 0 || doc.columnas <= 0) {
                throw new IllegalStateException("Documento de tablero sin dimensiones");
            }
            return doc;
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo leer el tablero almacenado", e);
        }
    }

    public String escribir(TableroJson doc) {
        try {
            return objectMapper.writeValueAsString(doc);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("No se pudo serializar el tablero", e);
        }
    }

    public Tablero aTablero(TableroJson doc) {
        Tablero t = new Tablero(doc.filas, doc.columnas);
        for (int[] p : seguro(doc.minas)) {
            if (valida(t, p)) {
                t.ponerMina(p[0], p[1]);
            }
        }
        for (int[] p : seguro(doc.reveladas)) {
            if (valida(t, p)) {
                t.revelar(p[0], p[1]);
            }
        }
        for (int[] p : seguro(doc.banderas)) {
            if (valida(t, p)) {
                t.setBandera(p[0], p[1], true);
            }
        }
        if (doc.explotada != null && valida(t, doc.explotada)) {
            t.setExplotada(doc.explotada[0], doc.explotada[1]);
        }
        return t;
    }

    /** Copia el estado del tablero en memoria a las listas del documento JSON. */
    public void volcar(Tablero t, TableroJson doc) {
        List<int[]> minas = new ArrayList<>();
        List<int[]> reveladas = new ArrayList<>();
        List<int[]> banderas = new ArrayList<>();
        for (int f = 0; f < t.getFilas(); f++) {
            for (int c = 0; c < t.getColumnas(); c++) {
                if (t.tieneMina(f, c)) {
                    minas.add(new int[]{f, c});
                }
                if (t.estaRevelada(f, c)) {
                    reveladas.add(new int[]{f, c});
                }
                if (t.tieneBandera(f, c)) {
                    banderas.add(new int[]{f, c});
                }
            }
        }
        doc.filas = t.getFilas();
        doc.columnas = t.getColumnas();
        doc.numMinas = minas.size();
        doc.minas = minas;
        doc.reveladas = reveladas;
        doc.banderas = banderas;
        doc.explotada = t.getExplotada();
    }

    private static List<int[]> seguro(List<int[]> l) {
        return l == null ? List.of() : l;
    }

    private static boolean valida(Tablero t, int[] p) {
        return p != null && p.length == 2 && t.dentro(p[0], p[1]);
    }
}
