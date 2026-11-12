package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.model.dto.RankingFilaDTO;
import com.bachillerato.buscaminas.model.dto.RankingNivelDTO;
import com.bachillerato.buscaminas.model.juego.Nivel;
import com.bachillerato.buscaminas.repository.PartidaRepository;
import com.bachillerato.buscaminas.service.RankingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Ranking de mejores tiempos leído de la vista vw_ranking (MIN / GROUP BY). */
@Service
public class RankingServiceImpl implements RankingService {

    private final PartidaRepository partidaRepository;

    public RankingServiceImpl(PartidaRepository partidaRepository) {
        this.partidaRepository = partidaRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RankingNivelDTO> ranking(Long usuarioActualId, Nivel nivel) {
        List<Nivel> niveles = nivel == null ? List.of(Nivel.values()) : List.of(nivel);
        List<RankingNivelDTO> resultado = new ArrayList<>();
        for (Nivel n : niveles) {
            resultado.add(rankingDeNivel(usuarioActualId, n));
        }
        return resultado;
    }

    private RankingNivelDTO rankingDeNivel(Long usuarioActualId, Nivel nivel) {
        List<Object[]> filas = partidaRepository.rankingPorNivel(nivel.name());
        List<RankingFilaDTO> posiciones = new ArrayList<>();
        RankingFilaDTO mia = null;

        int posicion = 0;
        long tiempoAnterior = -1;
        for (int i = 0; i < filas.size(); i++) {
            Object[] r = filas.get(i);
            long mejor = ((Number) r[3]).longValue();
            // Empates en tiempo comparten la posición (1, 2, 2, 4...).
            if (mejor != tiempoAnterior) {
                posicion = i + 1;
                tiempoAnterior = mejor;
            }
            Long uid = ((Number) r[0]).longValue();
            boolean esMio = Objects.equals(uid, usuarioActualId);
            RankingFilaDTO fila = new RankingFilaDTO(
                    posicion,
                    uid,
                    (String) r[1],
                    mejor,
                    ((Number) r[4]).longValue(),
                    redondear(r[5] == null ? 0 : ((Number) r[5]).doubleValue()),
                    aFecha(r[6]),
                    esMio);
            posiciones.add(fila);
            if (esMio) {
                mia = fila;
            }
        }
        return new RankingNivelDTO(nivel.name(), nivel.getNombre(), posiciones, mia);
    }

    private static double redondear(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static LocalDateTime aFecha(Object valor) {
        if (valor instanceof LocalDateTime ldt) {
            return ldt;
        }
        if (valor instanceof java.sql.Timestamp ts) {
            return ts.toLocalDateTime();
        }
        if (valor instanceof java.util.Date d) {
            return LocalDateTime.ofInstant(d.toInstant(), ZoneId.systemDefault());
        }
        return null;
    }
}
