package com.bachillerato.buscaminas.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Principal que se guarda en la sesión HTTP. Además del nombre de usuario
 * conserva el id, para comprobar la propiedad de partidas sin ir a la BD.
 */
public class UsuarioAutenticado extends User {

    private static final long serialVersionUID = 1L;

    private final Long id;

    public UsuarioAutenticado(Long id, String nombreUsuario, String passwordHash, boolean activo,
                              Collection<? extends GrantedAuthority> roles) {
        super(nombreUsuario, passwordHash, activo, true, true, true, roles);
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
