package com.bachillerato.buscaminas.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Datos del formulario de registro, validados en el servidor con Bean Validation. */
public record RegistroRequest(

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 80, message = "El nombre admite como máximo 80 caracteres")
        String nombre,

        @NotBlank(message = "El nombre de usuario es obligatorio")
        @Size(min = 3, max = 40, message = "El nombre de usuario debe tener entre 3 y 40 caracteres")
        @Pattern(regexp = "^[A-Za-z0-9_.-]+$",
                message = "El nombre de usuario solo admite letras, números, punto, guion y guion bajo")
        String nombreUsuario,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato válido")
        @Size(max = 120, message = "El email admite como máximo 120 caracteres")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        @Size(min = 8, max = 128, message = "La contraseña debe tener al menos 8 caracteres")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "La contraseña debe contener al menos una letra y un número")
        String password,

        @NotBlank(message = "La confirmación de contraseña es obligatoria")
        String confirmacion
) {

    /** Regla a nivel de objeto: la confirmación debe coincidir con la contraseña. */
    @JsonIgnore
    @AssertTrue(message = "La confirmación no coincide con la contraseña")
    public boolean isConfirmacionValida() {
        if (password == null || confirmacion == null) {
            return true; // de esos casos se encargan @NotBlank
        }
        return password.equals(confirmacion);
    }

    @Override
    public String toString() {
        // Nunca imprimir contraseñas en logs.
        return "RegistroRequest[nombreUsuario=" + nombreUsuario + ", email=" + email + "]";
    }
}
