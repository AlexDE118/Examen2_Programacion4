package com.bachillerato.buscaminas.service.impl;

import com.bachillerato.buscaminas.config.UsuarioAutenticado;
import com.bachillerato.buscaminas.model.entity.Usuario;
import com.bachillerato.buscaminas.repository.UsuarioRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Carga el usuario para Spring Security (por nombre de usuario o email). */
@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario u = usuarioRepository.buscarPorLogin(login == null ? "" : login.trim())
                // El mensaje nunca llega al cliente: el controlador responde un error genérico.
                .orElseThrow(() -> new UsernameNotFoundException("Credenciales inválidas"));
        return new UsuarioAutenticado(u.getId(), u.getNombreUsuario(), u.getPasswordHash(), u.isActivo(),
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRol().name())));
    }
}
