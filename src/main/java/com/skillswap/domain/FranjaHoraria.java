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
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void ocupar() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void liberar() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
