package com.bachillerato.buscaminas.repository;

import com.bachillerato.buscaminas.model.entity.EstadoPartida;
import com.bachillerato.buscaminas.model.entity.Partida;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PartidaRepository extends JpaRepository<Partida, Long> {

    /** JPQL: ¿el usuario ya tiene una partida en ese estado? */
    @Query("select case when count(p) > 0 then true else false end "
            + "from Partida p where p.usuario.id = :usuarioId and p.estado = :estado")
    boolean existePorUsuarioYEstado(@Param("usuarioId") Long usuarioId, @Param("estado") EstadoPartida estado);

    /** JPQL: partidas del usuario en un estado, la más reciente primero. */
    @Query("select p from Partida p where p.usuario.id = :usuarioId and p.estado = :estado "
            + "order by p.fechaInicio desc, p.id desc")
    List<Partida> buscarPorUsuarioYEstado(@Param("usuarioId") Long usuarioId,
                                          @Param("estado") EstadoPartida estado);

    /**
     * JPQL: historial completo del usuario. No navega a «usuario», así que no
     * dispara una consulta extra por fila (sin problema N+1).
     */
    @Query("select p from Partida p where p.usuario.id = :usuarioId order by p.fechaInicio desc, p.id desc")
    List<Partida> historialDeUsuario(@Param("usuarioId") Long usuarioId);

    /**
     * SQL nativo sobre la vista vw_ranking (MIN + COUNT + AVG con GROUP BY por
     * usuario y nivel). Columnas: 0 usuario_id, 1 nombre_usuario, 2 nivel,
     * 3 mejor_tiempo_ms, 4 partidas_terminadas, 5 promedio_movimientos, 6 ultimo_juego.
     */
    @Query(value = "SELECT r.usuario_id, r.nombre_usuario, r.nivel, r.mejor_tiempo_ms, "
            + "r.partidas_terminadas, r.promedio_movimientos, r.ultimo_juego "
            + "FROM vw_ranking r WHERE r.nivel = :nivel "
            + "ORDER BY r.mejor_tiempo_ms ASC, r.ultimo_juego ASC, r.nombre_usuario ASC",
            nativeQuery = true)
    List<Object[]> rankingPorNivel(@Param("nivel") String nivel);
}
