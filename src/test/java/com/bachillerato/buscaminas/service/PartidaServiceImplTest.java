package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.exception.ApiException;
import com.bachillerato.buscaminas.model.dto.AccionRequest;
import com.bachillerato.buscaminas.model.dto.PartidaDTO;
import com.bachillerato.buscaminas.model.entity.Partida;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.model.juego.Nivel;
import com.bachillerato.buscaminas.model.juego.TipoAccion;
import com.bachillerato.buscaminas.repository.PartidaRepository;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import com.bachillerato.buscaminas.service.impl.MotorBuscaminas;
import com.bachillerato.buscaminas.service.impl.PartidaMapper;
import com.bachillerato.buscaminas.service.impl.PartidaServiceImpl;
import com.bachillerato.buscaminas.service.impl.TableroMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Prueba de la capa de servicio: la apertura en cadena se ejecuta en
 * PartidaServiceImpl, se persiste en el JSON y la respuesta sale filtrada.
 */
class PartidaServiceImplTest {

    private static final long DUENO = 1L;
    private static final long OTRO = 2L;
    private static final long PARTIDA_ID = 10L;

    /** 9x9 con una pared de minas en la columna 4. */
    private static final String TABLERO_PARED = "{\"filas\":9,\"columnas\":9,\"num_minas\":9,"
            + "\"minas\":[[0,4],[1,4],[2,4],[3,4],[4,4],[5,4],[6,4],[7,4],[8,4]],"
            + "\"reveladas\":[],\"banderas\":[],\"inicio_ms\":1000}";

    private PartidaRepository partidaRepository;
    private PartidaServiceImpl servicio;
    private Partida partida;

    @BeforeEach
    void preparar() {
        partidaRepository = mock(PartidaRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        Clock clock = Clock.fixed(Instant.ofEpochMilli(31_000), ZoneId.of("America/Costa_Rica"));
        MotorBuscaminas motor = new MotorBuscaminas();
        servicio = new PartidaServiceImpl(partidaRepository, usuarioRepository, motor,
                new TableroMapper(new ObjectMapper()), new PartidaMapper(motor), clock);

        Usuario dueno = new Usuario("Dueño", "dueno", "dueno@minas.local", "x".repeat(96));
        dueno.setId(DUENO);
        partida = new Partida(dueno, Nivel.PRINCIPIANTE, TABLERO_PARED, LocalDateTime.of(2026, 11, 10, 10, 0));
        partida.setId(PARTIDA_ID);

        when(partidaRepository.findById(PARTIDA_ID)).thenReturn(Optional.of(partida));
        when(partidaRepository.save(any(Partida.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void revelarCasillaVaciaAbreEnCadenaYNoFiltraMinas() {
        PartidaDTO dto = servicio.aplicarAccion(DUENO, PARTIDA_ID,
                new AccionRequest(TipoAccion.REVELAR, 0, 0));

        assertEquals("EN_CURSO", dto.estado());
        assertEquals(1, dto.movimientos());
        List<List<String>> celdas = dto.celdas();
        assertEquals("0", celdas.get(0).get(0));
        assertEquals("2", celdas.get(0).get(3));   // borde numérico
        assertEquals("3", celdas.get(4).get(3));
        for (int f = 0; f < 9; f++) {
            for (int c = 0; c < 4; c++) {
                assertFalse("O".equals(celdas.get(f).get(c)), "debería estar abierta (" + f + "," + c + ")");
            }
            for (int c = 4; c < 9; c++) {
                assertEquals("O", celdas.get(f).get(c)); // las minas siguen ocultas
            }
        }
        assertTrue(celdas.stream().flatMap(List::stream).noneMatch(v -> v.equals("M") || v.equals("X")));
        // El estado quedó persistido en el JSON de la entidad.
        assertTrue(partida.getTablero().contains("\"reveladas\":[[0,0]"));
    }

    @Test
    void ganarRegistraTiempoEstrellasYFechaFin() {
        servicio.aplicarAccion(DUENO, PARTIDA_ID, new AccionRequest(TipoAccion.REVELAR, 0, 0));
        PartidaDTO dto = servicio.aplicarAccion(DUENO, PARTIDA_ID, new AccionRequest(TipoAccion.REVELAR, 0, 8));

        assertEquals("TERMINADA", dto.estado());
        assertEquals("VICTORIA", dto.resultado());
        assertEquals(30_000L, dto.tiempoMs());      // 31 000 - 1 000 ms, medido en el servidor
        assertEquals(3, dto.estrellas());
        assertEquals(2, dto.movimientos());
        assertTrue(partida.getFechaFin() != null);
    }

    @Test
    void pisarMinaEsDerrotaSinTiempoDeRanking() {
        PartidaDTO dto = servicio.aplicarAccion(DUENO, PARTIDA_ID, new AccionRequest(TipoAccion.REVELAR, 2, 4));

        assertEquals("DERROTA", dto.resultado());
        assertEquals("X", dto.celdas().get(2).get(4));
        assertEquals("M", dto.celdas().get(3).get(4));
        assertNull(dto.tiempoMs());
        assertEquals(0, dto.estrellas());
    }

    @Test
    void otroUsuarioRecibe403YPartidaInexistente404() {
        ApiException e403 = assertThrows(ApiException.class, () -> servicio.aplicarAccion(OTRO, PARTIDA_ID,
                new AccionRequest(TipoAccion.REVELAR, 0, 0)));
        assertEquals(HttpStatus.FORBIDDEN, e403.getStatus());

        when(partidaRepository.findById(99L)).thenReturn(Optional.empty());
        ApiException e404 = assertThrows(ApiException.class, () -> servicio.detalle(DUENO, 99L));
        assertEquals(HttpStatus.NOT_FOUND, e404.getStatus());

        ApiException hist = assertThrows(ApiException.class, () -> servicio.historial(OTRO, DUENO));
        assertEquals(HttpStatus.FORBIDDEN, hist.getStatus());
    }

    @Test
    void accionSobrePartidaTerminadaEs400() {
        servicio.aplicarAccion(DUENO, PARTIDA_ID, new AccionRequest(TipoAccion.REVELAR, 2, 4));
        ApiException e = assertThrows(ApiException.class, () -> servicio.aplicarAccion(DUENO, PARTIDA_ID,
                new AccionRequest(TipoAccion.REVELAR, 0, 0)));
        assertEquals(HttpStatus.BAD_REQUEST, e.getStatus());
    }
}
