package com.skillswap.domain.calificacion;

import com.skillswap.domain.contratacion.Contratacion;

import java.util.UUID;

public class Calificacion {
    private UUID id;

    private UUID contratacionId;

    private String calificadorId;

    private String calificadoId;

    private int puntaje;

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
}
