package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.SkillCoins;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PoliticaPorAnticipacionTest {

    private final PoliticaPorAnticipacion politica =
            new PoliticaPorAnticipacion(Duration.ofHours(48), Duration.ofHours(24));
    private final SkillCoins total = new SkillCoins(100);

    @Test
    void con48h_reintegraTotal() {
        assertEquals(100, politica.calcularReintegro(total, Duration.ofHours(48)).getValor());
    }

    @Test
    void con24h_reintegraMitad() {
        assertEquals(50, politica.calcularReintegro(total, Duration.ofHours(24)).getValor());
    }

    @Test
    void con2h_noReintegra() {
        assertEquals(0, politica.calcularReintegro(total, Duration.ofHours(2)).getValor());
    }

    @Test
    void nuncaSuperaTotal() {
        assertEquals(100, politica.calcularReintegro(total, Duration.ofDays(30)).getValor());
    }

    @Test
    void limitesInvertidos_lanzan() {
        assertThrows(IllegalArgumentException.class,
                () -> new PoliticaPorAnticipacion(Duration.ofHours(1), Duration.ofHours(10)));
    }

    @Test
    void politicaSinReintegro_siempreCero() {
        assertEquals(0, new PoliticaSinReintegro().calcularReintegro(total, Duration.ofDays(10)).getValor());
    }
}