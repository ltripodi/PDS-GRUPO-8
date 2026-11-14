package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/* Aviso en la bandeja de un usuario. La bandeja se consulta por destinatario, de la mas reciente a la mas vieja. */
@Entity
@Table(name = "notificaciones",
        indexes = @Index(name = "idx_notificaciones_destinatario_fecha", columnList = "destinatario_id, fecha"))
public class Notificacion {
    @Id
    private UUID id;

    @Column(name = "destinatario_id", nullable = false)
    private String destinatarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 1000)
    private String detalle;

    @Column(nullable = false)
    private Instant fecha;

    private boolean leida;

    protected Notificacion() {
    }

    /* Sin fecha explicita: la del momento en que se crea. */
    public Notificacion(String destinatarioId, TipoEvento tipo, String titulo, String detalle) {
        this(destinatarioId, tipo, titulo, detalle, Instant.now());
    }

    /* La fecha es la del hecho que se notifica (el evento), no la del momento en que se procesa el aviso. */
    public Notificacion(String destinatarioId, TipoEvento tipo, String titulo, String detalle, Instant fecha) {
        if (destinatarioId == null || destinatarioId.isBlank()) {
            throw new IllegalArgumentException("El destinatario de la notificacion es obligatorio");
        }
        if (tipo == null || titulo == null || titulo.isBlank()) {
            throw new IllegalArgumentException("El tipo y el titulo de la notificacion son obligatorios");
        }
        if (fecha == null) {
            throw new IllegalArgumentException("La fecha de la notificacion es obligatoria");
        }
        this.id = UUID.randomUUID();
        this.destinatarioId = destinatarioId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.detalle = detalle == null ? "" : detalle;
        this.fecha = fecha;
        this.leida = false;
    }

    /* Idempotente: marcar dos veces deja el mismo resultado. */
    public void marcarLeida() {
        this.leida = true;
    }

    public boolean esDe(String userId) {
        return destinatarioId.equals(userId);
    }

    public UUID getId() {
        return id;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalle() {
        return detalle;
    }

    public Instant getFecha() {
        return fecha;
    }

    public boolean isLeida() {
        return leida;
    }
}