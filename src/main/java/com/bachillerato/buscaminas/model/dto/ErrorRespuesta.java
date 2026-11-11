package com.bachillerato.buscaminas.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/** Cuerpo JSON uniforme para todos los errores de la API. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorRespuesta(
        LocalDateTime fecha,
        int status,
        String error,
        String mensaje,
        String ruta,
        Map<String, String> campos
) {
    public static ErrorRespuesta de(int status, String error, String mensaje, String ruta) {
        return new ErrorRespuesta(LocalDateTime.now().withNano(0), status, error, mensaje, ruta, null);
    }

    public static ErrorRespuesta conCampos(int status, String error, String mensaje, String ruta,
                                           Map<String, String> campos) {
        return new ErrorRespuesta(LocalDateTime.now().withNano(0), status, error, mensaje, ruta, campos);
    }
}
