package com.bachillerato.buscaminas.repository;

import com.bachillerato.buscaminas.model.entity.Usuario;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByNombreUsuario(String nombreUsuario);

    boolean existsByEmail(String email);

    /** Inicio de sesión: acepta nombre de usuario o email (JPQL). */
    @Query("select u from Usuario u where lower(u.nombreUsuario) = lower(:valor) or lower(u.email) = lower(:valor)")
    Optional<Usuario> buscarPorLogin(@Param("valor") String valor);

    /**
     * SELECT ... FOR UPDATE sobre la fila del usuario. Serializa la creación de
     * partidas del mismo usuario para impedir dos partidas EN_CURSO a la vez.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u where u.id = :id")
    Optional<Usuario> bloquearPorId(@Param("id") Long id);
}
