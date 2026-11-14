package com.skillswap.domain;

import java.time.Instant;
import java.util.UUID;

public class Notificacion {
    private UUID id;

    private String destinatarioId;

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
