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
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean seSuperponeCon(DisponibilidadPerfil otra) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
