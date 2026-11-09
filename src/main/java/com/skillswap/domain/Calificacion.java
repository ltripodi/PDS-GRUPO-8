package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.util.UUID;

@Entity
@Table(name = "calificaciones", uniqueConstraints = @UniqueConstraint(columnNames = {"contratacion_id", "calificador_id"}))
public class Calificacion {
    @Id
    private UUID id;

    @Column(name = "contratacion_id", nullable = false)
    private UUID contratacionId;

    @Column(name = "calificador_id", nullable = false)
    private String calificadorId;

    @Column(name = "calificado_id", nullable = false)
    private String calificadoId;

    private int puntaje;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratacion_id", insertable = false, updatable = false)
    private Contratacion contratacion;

    protected Calificacion() {
    }

    public Calificacion(UUID contratacionId, String calificadorId, String calificadoId, int puntaje) {
        this.id = UUID.randomUUID();
        this.contratacionId = contratacionId;
        this.calificadorId = calificadorId;
        this.calificadoId = calificadoId;
        this.puntaje = puntaje;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContratacionId() {
        return contratacionId;
    }

    public String getCalificadorId() {
        return calificadorId;
    }

    public String getCalificadoId() {
        return calificadoId;
    }

    public int getPuntaje() {
        return puntaje;
    }

    public Contratacion getContratacion() {
        return contratacion;
    }

    public boolean esVerificada() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
