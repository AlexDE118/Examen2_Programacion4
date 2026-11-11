package com.bachillerato.buscaminas.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Tabla «usuario». La contraseña solo existe como hash PBKDF2-HMAC-SHA256
 * (sal de 16 bytes + hash de 32 bytes, en hexadecimal: 96 caracteres).
 */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 80)
    private String nombre;

    @Column(name = "nombre_usuario", nullable = false, length = 40, unique = true)
    private String nombreUsuario;

    @Column(name = "email", nullable = false, length = 120, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false)
    private Rol rol = Rol.JUGADOR;

    @Column(name = "activo", nullable = false)
    private boolean activo = true;

    @Column(name = "creado_en", nullable = false, updatable = false)
    private LocalDateTime creadoEn;

    /** Lado inverso de la relación; carga diferida para no traer el historial en cada login. */
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Partida> partidas = new ArrayList<>();

    protected Usuario() {
        // requerido por JPA
    }

    public Usuario(String nombre, String nombreUsuario, String email, String passwordHash) {
        this.nombre = nombre;
        this.nombreUsuario = nombreUsuario;
        this.email = email;
        this.passwordHash = passwordHash;
        this.rol = Rol.JUGADOR;
        this.activo = true;
        this.creadoEn = LocalDateTime.now().withNano(0);
    }

    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Rol getRol() { return rol; }
    public boolean isActivo() { return activo; }
    public LocalDateTime getCreadoEn() { return creadoEn; }
    public List<Partida> getPartidas() { return partidas; }

    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public void setRol(Rol rol) { this.rol = rol; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
}
