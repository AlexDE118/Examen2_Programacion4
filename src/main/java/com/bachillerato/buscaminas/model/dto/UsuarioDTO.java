package com.bachillerato.buscaminas.model.dto;

import com.bachillerato.buscaminas.model.entity.Usuario;

import java.time.LocalDateTime;

/** Datos públicos de un usuario: nunca incluye el hash de la contraseña. */
public record UsuarioDTO(
        Long id,
        String nombre,
        String nombreUsuario,
        String email,
        String rol,
        LocalDateTime creadoEn
) {
    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombre(), u.getNombreUsuario(), u.getEmail(),
                u.getRol().name(), u.getCreadoEn());
    }
}
