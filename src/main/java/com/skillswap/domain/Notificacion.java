package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notificaciones")
public class Notificacion {
    @Id
    private UUID id;

    @Column(name = "destinatario_id", nullable = false)
    private String destinatarioId;

    @Column(length = 1000)
    private String detalle;

    private boolean leida;

    private String titulo;

    private String tipo;

    private Instant fecha;

    protected Notificacion() {
    }

    public Notificacion(String destinatarioId, String titulo, String detalle, String tipo) {
        this.id = UUID.randomUUID();
        this.destinatarioId = destinatarioId;
        this.titulo = titulo;
        this.detalle = detalle;
        this.tipo = tipo;
        this.leida = false;
        this.fecha = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getDestinatarioId() {
        return destinatarioId;
    }

    public String getDetalle() {
        return detalle;
    }

    public boolean isLeida() {
        return leida;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getFecha() {
        return fecha;
    }

    public void marcarLeida() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
