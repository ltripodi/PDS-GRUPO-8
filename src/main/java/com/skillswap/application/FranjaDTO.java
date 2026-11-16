package com.skillswap.application;

import java.time.Instant;
import java.util.UUID;

/* Franja horaria como la ve la interfaz: lo necesario para elegirla al solicitar o reprogramar. */
public class FranjaDTO {
    private final UUID id;
    private final Instant inicio;
    private final Instant fin;
    private final int lugaresLibres;

    public FranjaDTO(UUID id, Instant inicio, Instant fin, int lugaresLibres) {
        this.id = id;
        this.inicio = inicio;
        this.fin = fin;
        this.lugaresLibres = lugaresLibres;
    }

    public UUID getId() {
        return id;
    }

    public Instant getInicio() {
        return inicio;
    }

    public Instant getFin() {
        return fin;
    }

    public int getLugaresLibres() {
        return lugaresLibres;
    }
}
