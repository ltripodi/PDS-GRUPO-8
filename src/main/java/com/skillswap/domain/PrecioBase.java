package com.skillswap.domain;

public class PrecioBase implements CalculadorPrecio {

    private final SkillCoins monto;

    public PrecioBase(SkillCoins monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto base es obligatorio");
        }
        this.monto = monto;
    }

    @Override
    public SkillCoins total() {
        return this.monto;
    }
}