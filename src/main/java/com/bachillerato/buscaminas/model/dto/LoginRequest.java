package com.bachillerato.buscaminas.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Credenciales de inicio de sesión. «usuario» acepta el nombre de usuario o el email. */
public record LoginRequest(
        @NotBlank(message = "Indique su usuario o email")
        @Size(max = 120)
        String usuario,

        @NotBlank(message = "Indique su contraseña")
        @Size(max = 128)
        String password
) {
    @Override
    public String toString() {
        return "LoginRequest[usuario=" + usuario + "]";
    }
}
