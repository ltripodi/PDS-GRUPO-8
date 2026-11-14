package com.skillswap.domain;

import java.util.UUID;

public class Promocion {
    private UUID id;

    private SkillCoins descuento;

    protected Promocion() {
    }

    public Promocion(SkillCoins descuento) {
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
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
