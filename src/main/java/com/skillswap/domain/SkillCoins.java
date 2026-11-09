package com.skillswap.domain;

import jakarta.persistence.Embeddable;

/** Value Object. */
@Embeddable
public class SkillCoins {
    private int valor;

    protected SkillCoins() {
    }

    public SkillCoins(int valor) {
        if (valor < 0) {
            throw new IllegalArgumentException("El valor no puede ser negativo");
        }
        this.valor = valor;
    }

    public int getValor() {
        return valor;
    }

    public SkillCoins sumar(SkillCoins otro) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public SkillCoins restar(SkillCoins otro) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
