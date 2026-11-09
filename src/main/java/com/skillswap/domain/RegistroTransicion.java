package com.skillswap.domain;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Id;

import java.time.Instant;

/** Inmutable. */
@Embeddable
public class RegistroTransicion {
    private Instant fecha;
    private String actorId;
    private String origen;
    private String destino;
    private String motivo;

    protected RegistroTransicion() {
    }

    public RegistroTransicion(String actorId, String origen, String destino, String motivo) {
        this.fecha = Instant.now();
        this.actorId = actorId;
        this.origen = origen;
        this.destino = destino;
        this.motivo = motivo;
    }

    public Instant getFecha() {
        return fecha;
    }

    public String getActorId() {
        return actorId;
    }

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getMotivo() {
        return motivo;
    }
}
