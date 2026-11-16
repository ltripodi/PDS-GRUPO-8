package com.skillswap.domain.precio;

import com.skillswap.domain.compartido.SkillCoins;

import java.util.UUID;

public class Promocion {
    private UUID id;

    private SkillCoins descuento;

    protected Promocion() {
    }

    public Promocion(SkillCoins descuento) {
        if (descuento == null) {
            throw new IllegalArgumentException("El descuento es obligatorio");
        }
        this.id = UUID.randomUUID();
        this.descuento = descuento;
    }

    public UUID getId() {
        return id;
    }

    public SkillCoins getDescuento() {
        return descuento;
    }

    public SkillCoins aplicar(SkillCoins monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        int restante = monto.getValor() - this.descuento.getValor();
        if (restante < 0) {
            restante = 0;
        }
        return new SkillCoins(restante);
    }
}