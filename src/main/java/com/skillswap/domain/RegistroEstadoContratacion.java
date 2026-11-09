package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "registro_estado_contratacion")
public class RegistroEstadoContratacion {
    @Id
    private UUID id;

    private Instant fecha;

    @Column(name = "actor_id")
    private UUID actorId;

    private String origen;

    private String destino;

    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contratacion_id", nullable = false)
    private Contratacion contratacion;

    protected RegistroEstadoContratacion() {
    }

    public RegistroEstadoContratacion(Contratacion contratacion, UUID actorId, String origen, String destino, String motivo) {
        this.id = UUID.randomUUID();
        this.contratacion = contratacion;
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

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getMotivo() {
        return motivo;
    }

    public Contratacion getContratacion() {
        return contratacion;
    }
}
