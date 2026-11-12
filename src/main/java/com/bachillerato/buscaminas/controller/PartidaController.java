package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.config.UsuarioAutenticado;
import com.bachillerato.buscaminas.model.dto.AccionRequest;
import com.bachillerato.buscaminas.model.dto.CrearPartidaRequest;
import com.bachillerato.buscaminas.model.dto.PartidaDTO;
import com.bachillerato.buscaminas.service.PartidaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/** Todas las rutas exigen sesión (401 sin ella, configurado en SecurityConfig). */
@RestController
@RequestMapping(value = "/api/partidas", produces = MediaType.APPLICATION_JSON_VALUE)
public class PartidaController {

    private final PartidaService partidaService;

    public PartidaController(PartidaService partidaService) {
        this.partidaService = partidaService;
    }

    /** 201 con el tablero inicial filtrado; 409 si ya hay una partida en curso. */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PartidaDTO> crear(@AuthenticationPrincipal UsuarioAutenticado yo,
                                            @Valid @RequestBody CrearPartidaRequest request) {
        PartidaDTO creada = partidaService.crear(yo.getId(), request.nivel());
        return ResponseEntity.created(URI.create("/api/partidas/" + creada.id())).body(creada);
    }

    /** 200 con la partida en curso; 404 si no hay. */
    @GetMapping("/actual")
    public PartidaDTO actual(@AuthenticationPrincipal UsuarioAutenticado yo) {
        return partidaService.actual(yo.getId());
    }

    /** 200 con el estado actualizado; 400 acción inválida o partida inactiva; 403 ajena; 404 inexistente. */
    @PostMapping(value = "/{id}/acciones", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PartidaDTO accion(@AuthenticationPrincipal UsuarioAutenticado yo,
                             @PathVariable("id") Long id,
                             @Valid @RequestBody AccionRequest accion) {
        return partidaService.aplicarAccion(yo.getId(), id, accion);
    }

    /** Ruta adicional: abandona la partida en curso (queda en el historial como ABANDONADA). */
    @PostMapping("/{id}/abandonar")
    public PartidaDTO abandonar(@AuthenticationPrincipal UsuarioAutenticado yo,
                                @PathVariable("id") Long id) {
        return partidaService.abandonar(yo.getId(), id);
    }

    /** 200 con el detalle; 403 si es de otro usuario; 404 si no existe. */
    @GetMapping("/{id}")
    public PartidaDTO detalle(@AuthenticationPrincipal UsuarioAutenticado yo,
                              @PathVariable("id") Long id) {
        return partidaService.detalle(yo.getId(), id);
    }
}
