package com.skillswap.domain.pagos;

import com.skillswap.domain.compartido.SkillCoins;

import java.time.Instant;
import java.util.UUID;

public class Movimiento {
    private UUID id;

    private String titularId;

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
