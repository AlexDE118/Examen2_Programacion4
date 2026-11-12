package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.config.UsuarioAutenticado;
import com.bachillerato.buscaminas.model.dto.RankingNivelDTO;
import com.bachillerato.buscaminas.model.juego.Nivel;
import com.bachillerato.buscaminas.service.RankingService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/ranking", produces = MediaType.APPLICATION_JSON_VALUE)
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    /** 200 con la tabla de posiciones por nivel y la posición del usuario actual. ?nivel= es opcional. */
    @GetMapping
    public List<RankingNivelDTO> ranking(@AuthenticationPrincipal UsuarioAutenticado yo,
                                         @RequestParam(name = "nivel", required = false) Nivel nivel) {
        return rankingService.ranking(yo.getId(), nivel);
    }
}
