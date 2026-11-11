package com.bachillerato.buscaminas.controller;

import com.bachillerato.buscaminas.config.UsuarioAutenticado;
import com.bachillerato.buscaminas.exception.ApiException;
import com.bachillerato.buscaminas.model.dto.LoginRequest;
import com.bachillerato.buscaminas.model.dto.RegistroRequest;
import com.bachillerato.buscaminas.model.dto.UsuarioDTO;
import com.bachillerato.buscaminas.service.AuthService;
import com.bachillerato.buscaminas.service.impl.IntentosLoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
public class AuthController {

    /** Mismo mensaje para usuario inexistente, contraseña errónea o cuenta inactiva. */
    private static final String CREDENCIALES_INVALIDAS = "Credenciales inválidas";

    private final AuthService authService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final IntentosLoginService intentosLogin;

    public AuthController(AuthService authService, AuthenticationManager authenticationManager,
                          SecurityContextRepository securityContextRepository,
                          IntentosLoginService intentosLogin) {
        this.authService = authService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.intentosLogin = intentosLogin;
    }

    /** 201 con el usuario creado; 400 si falla la validación; 409 si ya existe. */
    @PostMapping(value = "/registro", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<UsuarioDTO> registrar(@Valid @RequestBody RegistroRequest request) {
        UsuarioDTO creado = authService.registrar(request);
        return ResponseEntity.created(URI.create("/api/auth/me")).body(creado);
    }

    /** 200 con el usuario y la sesión iniciada; 401 genérico si las credenciales no sirven. */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public UsuarioDTO login(@Valid @RequestBody LoginRequest request,
                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        String ip = httpRequest.getRemoteAddr();
        if (intentosLogin.estaBloqueado(request.usuario(), ip)) {
            throw ApiException.demasiadosIntentos("Demasiados intentos fallidos. Espere 15 minutos e intente de nuevo");
        }

        Authentication auth;
        try {
            auth = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.usuario().trim(), request.password()));
        } catch (AuthenticationException e) {
            intentosLogin.registrarFallo(request.usuario(), ip);
            throw ApiException.noAutenticado(CREDENCIALES_INVALIDAS);
        }
        intentosLogin.registrarExito(request.usuario(), ip);

        // Protección contra fijación de sesión: se descarta la sesión previa y se crea una nueva.
        HttpSession anterior = httpRequest.getSession(false);
        if (anterior != null) {
            anterior.invalidate();
        }
        httpRequest.getSession(true);

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(auth);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, httpRequest, httpResponse);

        UsuarioAutenticado principal = (UsuarioAutenticado) auth.getPrincipal();
        return authService.obtener(principal.getId());
    }

    /** 204 e invalida la sesión en el servidor; 401 si no había sesión (lo responde Spring Security). */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession sesion = request.getSession(false);
        if (sesion != null) {
            sesion.invalidate();
        }
        SecurityContextHolder.clearContext();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UsuarioDTO me(@AuthenticationPrincipal UsuarioAutenticado yo) {
        if (yo == null) {
            throw ApiException.noAutenticado("Debe iniciar sesión");
        }
        return authService.obtener(yo.getId());
    }
}
