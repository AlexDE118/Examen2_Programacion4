package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.config.UsuarioAutenticado;
import com.bachillerato.buscaminas.model.dto.PartidaResumenDTO;
import com.bachillerato.buscaminas.service.PartidaService;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/api/usuarios", produces = MediaType.APPLICATION_JSON_VALUE)
public class UsuarioController {

    private final PartidaService partidaService;

    public UsuarioController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    /** 200 con el historial propio; 403 si se piden las partidas de otro usuario. */
    @GetMapping("/{id}/partidas")
    public List<PartidaResumenDTO> historial(@AuthenticationPrincipal UsuarioAutenticado yo,
                                             @PathVariable("id") Long id) {
        return partidaService.historial(yo.getId(), id);
    }
}
