package com.skillswap.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "movimientos")
public class Movimiento {
    @Id
    private UUID id;

    @Column(name = "titular_id", nullable = false)
    private String titularId;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "monto"))
    private SkillCoins monto;

    private String tipo;

    private Instant fecha;

    protected Movimiento() {
    }

    public Movimiento(String titularId, SkillCoins monto, String tipo) {
        this.id = UUID.randomUUID();
        this.titularId = titularId;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitularId() {
        return titularId;
    }

    public SkillCoins getMonto() {
        return monto;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getFecha() {
        return fecha;
    }
}
