package com.bachillerato.buscaminas.config;

import com.bachillerato.buscaminas.model.dto.ErrorRespuesta;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

import java.io.IOException;
import java.time.Clock;

/**
 * Seguridad basada en sesión HTTP (cookie MINASSESSION):
 * - registro, login y niveles son públicos; el resto de /api/** exige sesión;
 * - sin sesión se responde 401 en JSON; sin permiso, 403 en JSON;
 * - las contraseñas se codifican con PBKDF2-HMAC-SHA256 (16 bytes de sal, 600 000 iteraciones).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Pbkdf2PasswordEncoder("", 16, 600_000,
                Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256);
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    /** AuthenticationManager que usa UsuarioDetailsService + el PasswordEncoder PBKDF2. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuracion) throws Exception {
        return configuracion.getAuthenticationManager();
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   SecurityContextRepository contextRepository,
                                                   ObjectMapper objectMapper) throws Exception {
        http
                // API JSON consumida desde el mismo origen; la cookie de sesión es SameSite=Strict
                // (ver application.properties), lo que impide su envío desde sitios de terceros.
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable) // el logout lo hace AuthController
                .securityContext(sc -> sc.securityContextRepository(contextRepository))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/auth/registro", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/niveles").permitAll()
                        .requestMatchers("/api/**").authenticated()
                        // Frontend estático (index.html, css, js) y página de error.
                        .anyRequest().permitAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(entryPoint401(objectMapper))
                        .accessDeniedHandler(accessDenied403(objectMapper)));
        return http.build();
    }

    private AuthenticationEntryPoint entryPoint401(ObjectMapper om) {
        return (request, response, authException) ->
                escribir(response, om, HttpStatus.UNAUTHORIZED, "Debe iniciar sesión", request.getRequestURI());
    }

    private AccessDeniedHandler accessDenied403(ObjectMapper om) {
        return (request, response, deniedException) ->
                escribir(response, om, HttpStatus.FORBIDDEN, "No tiene permiso para este recurso",
                        request.getRequestURI());
    }

    private static void escribir(HttpServletResponse response, ObjectMapper om, HttpStatus st,
                                 String mensaje, String ruta) throws IOException {
        response.setStatus(st.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        om.writeValue(response.getOutputStream(), ErrorRespuesta.de(st.value(), st.getReasonPhrase(), mensaje, ruta));
    }
}
