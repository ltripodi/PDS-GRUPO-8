package com.skillswap.domain;

/** Value Object. */
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
        if (otro == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        return new SkillCoins(this.valor + otro.valor);
    }

    public SkillCoins restar(SkillCoins otro) {
        if (otro == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        if (otro.valor > this.valor) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        return new SkillCoins(this.valor - otro.valor);
    }
}