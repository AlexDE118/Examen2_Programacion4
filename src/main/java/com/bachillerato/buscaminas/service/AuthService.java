package com.bachillerato.buscaminas.service;

import com.bachillerato.buscaminas.model.dto.RegistroRequest;
import com.bachillerato.buscaminas.model.dto.UsuarioDTO;

public interface AuthService {

    /** Crea la cuenta; 409 si el nombre de usuario o el email ya existen. */
    UsuarioDTO registrar(RegistroRequest request);

    /** Datos del usuario por id; 401 si ya no existe o está inactivo. */
    UsuarioDTO obtener(Long usuarioId);
}
