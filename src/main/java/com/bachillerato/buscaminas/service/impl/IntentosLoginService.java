package com.bachillerato.buscaminas.service.impl;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno contra fuerza bruta: tras 5 intentos fallidos para el mismo usuario
 * desde la misma IP, se bloquea el inicio de sesión durante 15 minutos.
 * Se aplica igual exista o no la cuenta, así no sirve para enumerar usuarios.
 */
@Component
public class IntentosLoginService {

    static final int MAX_INTENTOS = 5;
    static final long BLOQUEO_MS = 15 * 60 * 1000L;

    private final Map<String, Registro> intentos = new ConcurrentHashMap<>();
    private final Clock clock;

    public IntentosLoginService(Clock clock) {
        this.clock = clock;
    }

    public boolean estaBloqueado(String usuario, String ip) {
        Registro r = intentos.get(clave(usuario, ip));
        if (r == null) {
            return false;
        }
        if (r.bloqueadoHasta > 0 && clock.millis() < r.bloqueadoHasta) {
            return true;
        }
        if (r.bloqueadoHasta > 0) {
            intentos.remove(clave(usuario, ip)); // el bloqueo ya venció
        }
        return false;
    }

    public void registrarFallo(String usuario, String ip) {
        intentos.compute(clave(usuario, ip), (k, r) -> {
            Registro nuevo = r == null ? new Registro() : r;
            nuevo.fallos++;
            if (nuevo.fallos >= MAX_INTENTOS) {
                nuevo.bloqueadoHasta = clock.millis() + BLOQUEO_MS;
            }
            return nuevo;
        });
    }

    public void registrarExito(String usuario, String ip) {
        intentos.remove(clave(usuario, ip));
    }

    private String clave(String usuario, String ip) {
        return (usuario == null ? "" : usuario.trim().toLowerCase(Locale.ROOT)) + "|" + ip;
    }

    private static final class Registro {
        int fallos;
        long bloqueadoHasta;
    }
}
