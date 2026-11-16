package com.skillswap.domain;

public class PrecioConPromocion implements CalculadorPrecio {

    private final CalculadorPrecio componente;
    private final Promocion promocion;

    public PrecioConPromocion(CalculadorPrecio componente, Promocion promocion) {
        if (componente == null || promocion == null) {
            throw new IllegalArgumentException("Se requiere un calculador previo y una promoción");
        }
        this.componente = componente;
        this.promocion = promocion;
    }

    @Override
    public SkillCoins total() {
        return promocion.aplicar(componente.total());
    }
}