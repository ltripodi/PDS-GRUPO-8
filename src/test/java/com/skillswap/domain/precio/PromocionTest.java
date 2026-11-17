package com.skillswap.domain;

import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.precio.CalculadorPrecio;
import com.skillswap.domain.precio.PrecioBase;
import com.skillswap.domain.precio.PrecioConPromocion;
import com.skillswap.domain.precio.Promocion;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PromocionTest {

    @Test
    void aplicar_descuenta() {
        Promocion p = new Promocion(new SkillCoins(20));
        assertEquals(80, p.aplicar(new SkillCoins(100)).getValor());
    }

    @Test
    void aplicar_descuentoMayorAlMonto_seClampeaACero() {
        Promocion p = new Promocion(new SkillCoins(200));
        assertEquals(0, p.aplicar(new SkillCoins(50)).getValor());
    }

    @Test
    void precioConPromocion_sobrePrecioBase() {
        CalculadorPrecio base = new PrecioBase(new SkillCoins(100));
        CalculadorPrecio conPromo = new PrecioConPromocion(base, new Promocion(new SkillCoins(15)));
        assertEquals(85, conPromo.total().getValor());
    }
}