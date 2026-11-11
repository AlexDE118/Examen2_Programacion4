package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.exception.ApiException;
import com.bachillerato.buscaminas.model.dto.RegistroRequest;
import com.bachillerato.buscaminas.model.dto.UsuarioDTO;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import com.bachillerato.buscaminas.service.AuthService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UsuarioDTO registrar(RegistroRequest req) {
        String nombreUsuario = req.nombreUsuario().trim();
        String email = req.email().trim().toLowerCase(Locale.ROOT);

        if (usuarioRepository.existsByNombreUsuario(nombreUsuario)) {
            throw ApiException.conflicto("El nombre de usuario ya está registrado");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw ApiException.conflicto("El email ya está registrado");
        }

        // PBKDF2-HMAC-SHA256: 32 hex de sal + 64 hex de hash = 96 caracteres.
        String hash = passwordEncoder.encode(req.password());
        Usuario nuevo = new Usuario(req.nombre().trim(), nombreUsuario, email, hash);
        try {
            nuevo = usuarioRepository.saveAndFlush(nuevo);
        } catch (DataIntegrityViolationException e) {
            // Dos registros simultáneos con el mismo dato: la restricción UNIQUE decide.
            throw ApiException.conflicto("El nombre de usuario o el email ya están registrados");
        }
        return UsuarioDTO.de(nuevo);
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioDTO obtener(Long usuarioId) {
        return usuarioRepository.findById(usuarioId)
                .filter(Usuario::isActivo)
                .map(UsuarioDTO::de)
                .orElseThrow(() -> ApiException.noAutenticado("Debe iniciar sesión"));
    }
}
