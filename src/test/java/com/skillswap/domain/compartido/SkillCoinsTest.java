package com.skillswap.domain;

import com.skillswap.domain.compartido.SkillCoins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SkillCoinsTest {

    @Test
    void sumar_retornaNuevoValor() {
        assertEquals(15, new SkillCoins(10).sumar(new SkillCoins(5)).getValor());
    }

    @Test
    void restar_exitoso() {
        assertEquals(5, new SkillCoins(10).restar(new SkillCoins(5)).getValor());
    }

    @Test
    void restar_saldoInsuficiente_lanza() {
        assertThrows(IllegalStateException.class,
                () -> new SkillCoins(5).restar(new SkillCoins(10)));
    }

    @Test
    void constructor_negativo_lanza() {
        assertThrows(IllegalArgumentException.class, () -> new SkillCoins(-1));
    }
}