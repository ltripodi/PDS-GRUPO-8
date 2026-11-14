package com.skillswap.domain;

import java.time.Instant;
import java.util.UUID;

public class RegistroEstadoPublicacion {
    private UUID id;

    private Instant fecha;

    private UUID actorId;

    private EstadoPublicacion origen;

    private EstadoPublicacion destino;

    private String motivo;

    private Publicacion publicacion;

    protected RegistroEstadoPublicacion() {
    }

    public RegistroEstadoPublicacion(Publicacion publicacion, UUID actorId, EstadoPublicacion origen,
                                     EstadoPublicacion destino, String motivo) {
        this.id = UUID.randomUUID();
        this.publicacion = publicacion;
        this.fecha = Instant.now();
        this.actorId = actorId;
        this.origen = origen;
        this.destino = destino;
        this.motivo = motivo;
    }

    public UUID getId() {
        return id;
    }

    public Instant getFecha() {
        return fecha;
    }

    public UUID getActorId() {
        return actorId;
    }

    public EstadoPublicacion getOrigen() {
        return origen;
    }

    public EstadoPublicacion getDestino() {
        return destino;
    }

    public String getMotivo() {
        return motivo;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }
}
