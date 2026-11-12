package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.model.dto.RankingNivelDTO;
import com.bachillerato.buscaminas.model.juego.Nivel;

import java.util.List;

public interface RankingService {

    /** Ranking de todos los niveles, o solo del indicado si nivel != null. */
    List<RankingNivelDTO> ranking(Long usuarioActualId, Nivel nivel);
}
