package com.skillswap.domain;

public class PrecioBase implements CalculadorPrecio {
    private SkillCoins monto;

    @Override
    public SkillCoins total() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
