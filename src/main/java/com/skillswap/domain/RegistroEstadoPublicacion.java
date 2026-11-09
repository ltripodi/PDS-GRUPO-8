package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registro_estado_publicacion")
public class RegistroEstadoPublicacion {
    @Id
    private UUID id;

    private Instant fecha;

    @Column(name = "actor_id")
    private UUID actorId;

    @Enumerated(EnumType.STRING)
    private EstadoPublicacion origen;

    @Enumerated(EnumType.STRING)
    private EstadoPublicacion destino;

    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
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
