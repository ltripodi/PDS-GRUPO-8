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
        if (contratacionId == null) {
            throw new IllegalArgumentException("La contratacion es obligatoria");
        }
        if (calificadorId == null || calificadorId.isBlank() || calificadoId == null || calificadoId.isBlank()) {
            throw new IllegalArgumentException("Calificador y calificado son obligatorios");
        }
        if (calificadorId.equals(calificadoId)) {
            throw new IllegalArgumentException("Una persona no puede calificarse a si misma");
        }
        if (puntaje < 1 || puntaje > 5) {
            throw new IllegalArgumentException("El puntaje debe estar entre 1 y 5");
        }
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
        return contratacion != null && "COMPLETADA".equals(contratacion.getEstado());
    }
}
