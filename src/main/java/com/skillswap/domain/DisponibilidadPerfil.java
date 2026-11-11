package com.skillswap.domain;

import jakarta.persistence.Embeddable;

import java.time.Instant;

/** Value Object. */
@Embeddable
public class DisponibilidadPerfil {
    private Instant inicio;
    private Instant fin;

    protected DisponibilidadPerfil() {
    }

    public DisponibilidadPerfil(Instant inicio, Instant fin) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La disponibilidad debe tener inicio anterior al fin");
        }
        this.inicio = inicio;
        this.fin = fin;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public boolean esValida() {
        return inicio != null && fin != null && inicio.isBefore(fin);
    }

    public boolean seSuperponeCon(DisponibilidadPerfil otra) {
        if (otra == null || !esValida() || !otra.esValida()) {
            return false;
        }
        return inicio.isBefore(otra.fin) && otra.inicio.isBefore(fin);
    }
}
