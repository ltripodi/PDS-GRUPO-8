package com.skillswap.domain;

public class PrecioConPromocion implements CalculadorPrecio {
    private CalculadorPrecio componente;
    private Promocion promocion;

    @Override
    public SkillCoins total() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
