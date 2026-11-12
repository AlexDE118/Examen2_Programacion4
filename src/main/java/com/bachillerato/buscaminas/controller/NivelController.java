package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.model.dto.NivelDTO;
import com.bachillerato.buscaminas.model.juego.Nivel;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping(value = "/api/niveles", produces = MediaType.APPLICATION_JSON_VALUE)
public class NivelController {

    /** 200 con los niveles definidos como constantes en el backend. */
    @GetMapping
    public List<NivelDTO> listar() {
        return Arrays.stream(Nivel.values()).map(NivelDTO::de).toList();
    }
}
