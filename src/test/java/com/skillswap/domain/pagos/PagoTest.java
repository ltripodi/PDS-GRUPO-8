package com.skillswap.domain;

import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.pagos.Pago;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PagoTest {

    @Test
    void registrarReintegro_acumula() {
        Pago p = new Pago(UUID.randomUUID(), new SkillCoins(100));
        p.registrarReintegro(new SkillCoins(30));
        p.registrarReintegro(new SkillCoins(20));
        assertEquals(50, p.getTotalReintegrado().getValor());
    }

    @Test
    void registrarReintegro_noSuperaTotal() {
        Pago p = new Pago(UUID.randomUUID(), new SkillCoins(100));
        p.registrarReintegro(new SkillCoins(80));
        assertThrows(IllegalStateException.class, () -> p.registrarReintegro(new SkillCoins(30)));
    }
}