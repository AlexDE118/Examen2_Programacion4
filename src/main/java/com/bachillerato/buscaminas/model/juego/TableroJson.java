package com.bachillerato.buscaminas.model.juego;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/**
 * Forma del documento guardado en la columna JSON partida.tablero. Es compatible
 * con la partida semilla de minas.sql:
 * <pre>
 * {"filas":9,"columnas":9,"num_minas":10,
 *  "minas":[[0,5],...], "reveladas":[[0,0],...], "banderas":[[0,5],...]}
 * </pre>
 * Se agregan campos opcionales: inicio_ms (instante exacto de inicio, para medir
 * el tiempo en milisegundos), duracion_ms, explotada y resultado.
 * Este objeto es interno: jamás se devuelve al navegador.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TableroJson {

    @JsonProperty("filas")
    public int filas;

    @JsonProperty("columnas")
    public int columnas;

    @JsonProperty("num_minas")
    public int numMinas;

    @JsonProperty("minas")
    public List<int[]> minas = new ArrayList<>();

    @JsonProperty("reveladas")
    public List<int[]> reveladas = new ArrayList<>();

    @JsonProperty("banderas")
    public List<int[]> banderas = new ArrayList<>();

    @JsonProperty("explotada")
    public int[] explotada;

    @JsonProperty("inicio_ms")
    public Long inicioMs;

    @JsonProperty("duracion_ms")
    public Long duracionMs;

    @JsonProperty("resultado")
    public String resultado;
}
