package com.bachillerato.buscaminas.model.entity;

import com.bachillerato.buscaminas.model.juego.Nivel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

/**
 * Tabla «partida». El estado completo del tablero (minas, reveladas y banderas)
 * vive en la columna JSON «tablero»; nunca se envía tal cual al navegador.
 */
@Entity
@Table(name = "partida")
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "nivel", nullable = false, length = 20)
    private Nivel nivel;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private EstadoPartida estado = EstadoPartida.EN_CURSO;

    /** Solo se llena en las victorias: es el tiempo que usa el ranking. */
    @Column(name = "tiempo_ms")
    private Integer tiempoMs;

    @Column(name = "movimientos", nullable = false)
    private Integer movimientos = 0;

    @Column(name = "estrellas", nullable = false)
    private Integer estrellas = 0;

    /** Documento JSON del tablero (columna de tipo JSON en MySQL). */
    @Column(name = "tablero", nullable = false, columnDefinition = "json")
    private String tablero;

    /** Bloqueo optimista: dos acciones simultáneas sobre la misma partida no se pisan. */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "fecha_inicio", nullable = false, updatable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    protected Partida() {
        // requerido por JPA
    }

    public Partida(Usuario usuario, Nivel nivel, String tablero, LocalDateTime fechaInicio) {
        this.usuario = usuario;
        this.nivel = nivel;
        this.tablero = tablero;
        this.fechaInicio = fechaInicio;
        this.estado = EstadoPartida.EN_CURSO;
        this.movimientos = 0;
        this.estrellas = 0;
    }

    public boolean perteneceA(Long usuarioId) {
        return usuario != null && usuario.getId() != null && usuario.getId().equals(usuarioId);
    }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public Nivel getNivel() { return nivel; }
    public EstadoPartida getEstado() { return estado; }
    public Integer getTiempoMs() { return tiempoMs; }
    public Integer getMovimientos() { return movimientos; }
    public Integer getEstrellas() { return estrellas; }
    public String getTablero() { return tablero; }
    public Long getVersion() { return version; }
    public LocalDateTime getFechaInicio() { return fechaInicio; }
    public LocalDateTime getFechaFin() { return fechaFin; }

    public void setId(Long id) { this.id = id; }
    public void setEstado(EstadoPartida estado) { this.estado = estado; }
    public void setTiempoMs(Integer tiempoMs) { this.tiempoMs = tiempoMs; }
    public void setMovimientos(Integer movimientos) { this.movimientos = movimientos; }
    public void setEstrellas(Integer estrellas) { this.estrellas = estrellas; }
    public void setTablero(String tablero) { this.tablero = tablero; }
    public void setFechaFin(LocalDateTime fechaFin) { this.fechaFin = fechaFin; }
}
