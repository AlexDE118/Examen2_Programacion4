package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.exception.ApiException;
import com.bachillerato.buscaminas.model.dto.AccionRequest;
import com.bachillerato.buscaminas.model.dto.PartidaDTO;
import com.bachillerato.buscaminas.model.dto.PartidaResumenDTO;
import com.bachillerato.buscaminas.model.entity.EstadoPartida;
import com.bachillerato.buscaminas.model.entity.Partida;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.model.juego.Nivel;
import com.bachillerato.buscaminas.model.juego.ResultadoMovimiento;
import com.bachillerato.buscaminas.model.juego.ResultadoPartida;
import com.bachillerato.buscaminas.model.juego.Tablero;
import com.bachillerato.buscaminas.model.juego.TableroJson;
import com.bachillerato.buscaminas.model.juego.TipoAccion;
import com.bachillerato.buscaminas.repository.PartidaRepository;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import com.bachillerato.buscaminas.service.PartidaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

@Service
public class PartidaServiceImpl implements PartidaService {

    private static final int MAX_MOVIMIENTOS = 65_535; // SMALLINT UNSIGNED

    private final PartidaRepository partidaRepository;
    private final UsuarioRepository usuarioRepository;
    private final MotorBuscaminas motor;
    private final TableroMapper tableroMapper;
    private final PartidaMapper partidaMapper;
    private final Clock clock;

    public PartidaServiceImpl(PartidaRepository partidaRepository, UsuarioRepository usuarioRepository,
                              MotorBuscaminas motor, TableroMapper tableroMapper,
                              PartidaMapper partidaMapper, Clock clock) {
        this.partidaRepository = partidaRepository;
        this.usuarioRepository = usuarioRepository;
        this.motor = motor;
        this.tableroMapper = tableroMapper;
        this.partidaMapper = partidaMapper;
        this.clock = clock;
    }

    /**
     * Crea la partida y coloca las minas. La fila del usuario se bloquea con
     * SELECT ... FOR UPDATE para que dos peticiones simultáneas no creen dos
     * partidas EN_CURSO.
     */
    @Override
    @Transactional
    public PartidaDTO crear(Long usuarioId, Nivel nivel) {
        Usuario usuario = usuarioRepository.bloquearPorId(usuarioId)
                .orElseThrow(() -> ApiException.noAutenticado("Debe iniciar sesión"));

        if (partidaRepository.existePorUsuarioYEstado(usuarioId, EstadoPartida.EN_CURSO)) {
            throw ApiException.conflicto("Ya tiene una partida en curso: termínela o abandónela antes de iniciar otra");
        }

        Tablero tablero = motor.crearTablero(nivel);
        TableroJson doc = new TableroJson();
        doc.inicioMs = clock.millis();
        tableroMapper.volcar(tablero, doc);

        Partida partida = new Partida(usuario, nivel, tableroMapper.escribir(doc), ahora());
        partida = partidaRepository.save(partida);
        return partidaMapper.aDto(partida, tablero, doc, clock.millis());
    }

    @Override
    @Transactional(readOnly = true)
    public PartidaDTO actual(Long usuarioId) {
        Partida partida = partidaRepository.buscarPorUsuarioYEstado(usuarioId, EstadoPartida.EN_CURSO)
                .stream().findFirst()
                .orElseThrow(() -> ApiException.noEncontrado("No tiene una partida en curso"));
        return aDto(partida);
    }

    @Override
    @Transactional
    public PartidaDTO aplicarAccion(Long usuarioId, Long partidaId, AccionRequest accion) {
        Partida partida = cargarPropia(usuarioId, partidaId);
        if (partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw ApiException.solicitudInvalida("La partida ya no está activa");
        }

        TableroJson doc = tableroMapper.leer(partida.getTablero());
        Tablero tablero = tableroMapper.aTablero(doc);
        int f = accion.fila();
        int c = accion.columna();

        ResultadoMovimiento resultado;
        if (accion.tipo() == TipoAccion.REVELAR) {
            resultado = motor.revelar(tablero, f, c);
        } else {
            motor.alternarBandera(tablero, f, c);
            resultado = ResultadoMovimiento.CONTINUA;
        }

        partida.setMovimientos(Math.min(MAX_MOVIMIENTOS, partida.getMovimientos() + 1));
        if (resultado == ResultadoMovimiento.VICTORIA) {
            finalizar(partida, doc, ResultadoPartida.VICTORIA);
        } else if (resultado == ResultadoMovimiento.MINA) {
            finalizar(partida, doc, ResultadoPartida.DERROTA);
        }

        tableroMapper.volcar(tablero, doc);
        partida.setTablero(tableroMapper.escribir(doc));
        partida = partidaRepository.save(partida);
        return partidaMapper.aDto(partida, tablero, doc, clock.millis());
    }

    @Override
    @Transactional
    public PartidaDTO abandonar(Long usuarioId, Long partidaId) {
        Partida partida = cargarPropia(usuarioId, partidaId);
        if (partida.getEstado() != EstadoPartida.EN_CURSO) {
            throw ApiException.solicitudInvalida("La partida ya no está activa");
        }
        TableroJson doc = tableroMapper.leer(partida.getTablero());
        finalizar(partida, doc, ResultadoPartida.ABANDONADA);
        partida.setTablero(tableroMapper.escribir(doc));
        partida = partidaRepository.save(partida);
        return partidaMapper.aDto(partida, tableroMapper.aTablero(doc), doc, clock.millis());
    }

    @Override
    @Transactional(readOnly = true)
    public PartidaDTO detalle(Long usuarioId, Long partidaId) {
        return aDto(cargarPropia(usuarioId, partidaId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PartidaResumenDTO> historial(Long usuarioSolicitante, Long usuarioConsultado) {
        if (!Objects.equals(usuarioSolicitante, usuarioConsultado)) {
            throw ApiException.prohibido("Solo puede consultar su propio historial");
        }
        return partidaRepository.historialDeUsuario(usuarioConsultado).stream()
                .map(p -> partidaMapper.aResumen(p, leerSinFallar(p)))
                .toList();
    }

    // ------------------------------------------------------------------ auxiliares

    /** 404 si la partida no existe; 403 si pertenece a otro usuario. */
    private Partida cargarPropia(Long usuarioId, Long partidaId) {
        Partida partida = partidaRepository.findById(partidaId)
                .orElseThrow(() -> ApiException.noEncontrado("La partida no existe"));
        if (!partida.perteneceA(usuarioId)) {
            throw ApiException.prohibido("La partida pertenece a otro usuario");
        }
        return partida;
    }

    /**
     * Cierra la partida con fecha_fin y duración medida en el servidor.
     * Solo la victoria guarda tiempo_ms y estrellas (1-3), que son los datos
     * que usa la vista vw_ranking; la derrota guarda 0 estrellas.
     */
    private void finalizar(Partida partida, TableroJson doc, ResultadoPartida resultado) {
        long ahoraMs = clock.millis();
        long inicioMs = doc.inicioMs != null ? doc.inicioMs
                : partida.getFechaInicio().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long duracion = Math.max(0, ahoraMs - inicioMs);

        LocalDateTime fin = ahora();
        if (fin.isBefore(partida.getFechaInicio())) {
            fin = partida.getFechaInicio();
        }
        partida.setFechaFin(fin);
        doc.duracionMs = duracion;
        doc.resultado = resultado.name();

        switch (resultado) {
            case VICTORIA -> {
                partida.setEstado(EstadoPartida.TERMINADA);
                partida.setTiempoMs((int) Math.min(Integer.MAX_VALUE, duracion));
                partida.setEstrellas(partida.getNivel().estrellasPara(duracion));
            }
            case DERROTA -> {
                partida.setEstado(EstadoPartida.TERMINADA);
                partida.setTiempoMs(null);
                partida.setEstrellas(0);
            }
            default -> {
                partida.setEstado(EstadoPartida.ABANDONADA);
                partida.setTiempoMs(null);
                partida.setEstrellas(0);
            }
        }
    }

    private PartidaDTO aDto(Partida partida) {
        TableroJson doc = tableroMapper.leer(partida.getTablero());
        return partidaMapper.aDto(partida, tableroMapper.aTablero(doc), doc, clock.millis());
    }

    private TableroJson leerSinFallar(Partida p) {
        try {
            return tableroMapper.leer(p.getTablero());
        } catch (RuntimeException e) {
            return null; // el historial se muestra aunque un tablero esté dañado
        }
    }

    /** DATETIME de MySQL no guarda fracciones: se trunca a segundos. */
    private LocalDateTime ahora() {
        return LocalDateTime.now(clock).withNano(0);
    }
}
