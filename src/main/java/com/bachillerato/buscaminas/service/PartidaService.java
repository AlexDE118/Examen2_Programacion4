package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.model.dto.AccionRequest;
import com.bachillerato.buscaminas.model.dto.PartidaDTO;
import com.bachillerato.buscaminas.model.dto.PartidaResumenDTO;
import com.bachillerato.buscaminas.model.juego.Nivel;

import java.util.List;

public interface PartidaService {

    PartidaDTO crear(Long usuarioId, Nivel nivel);

    PartidaDTO actual(Long usuarioId);

    PartidaDTO aplicarAccion(Long usuarioId, Long partidaId, AccionRequest accion);

    PartidaDTO abandonar(Long usuarioId, Long partidaId);

    PartidaDTO detalle(Long usuarioId, Long partidaId);

    List<PartidaResumenDTO> historial(Long usuarioSolicitante, Long usuarioConsultado);
}
