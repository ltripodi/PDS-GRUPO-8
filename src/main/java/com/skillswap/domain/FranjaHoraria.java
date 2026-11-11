package com.skillswap.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "franjas_horarias")
public class FranjaHoraria {
    @Id
    private UUID id;

    private Instant inicio;

    private Instant fin;

    private int cupo;

    private int ocupados;

    @Version
    private long version;

    protected FranjaHoraria() {
    }

    public FranjaHoraria(Instant inicio, Instant fin, int cupo) {
        if (inicio == null || fin == null || !inicio.isBefore(fin)) {
            throw new IllegalArgumentException("La franja debe tener inicio anterior al fin");
        }
        if (cupo <= 0) {
            throw new IllegalArgumentException("El cupo debe ser positivo");
        }
        this.id = UUID.randomUUID();
        this.inicio = inicio;
        this.fin = fin;
        this.cupo = cupo;
        this.ocupados = 0;
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

    public int getCupo() {
        return cupo;
    }

    public int getOcupados() {
        return ocupados;
    }

    public long getVersion() {
        return version;
    }

    public boolean hayCupo() {
        return ocupados < cupo;
    }

    public void ocupar() {
        if (!hayCupo()) {
            throw new IllegalStateException("La franja no tiene cupo disponible");
        }
        ocupados++;
    }

    public void liberar() {
        if (ocupados == 0) {
            throw new IllegalStateException("La franja no tiene cupos ocupados para liberar");
        }
        ocupados--;
    }
}
