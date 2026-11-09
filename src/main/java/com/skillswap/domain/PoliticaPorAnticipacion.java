package com.skillswap.domain;

import java.time.Duration;

public class PoliticaPorAnticipacion implements PoliticaCancelacion {
    private Duration limiteTotal;
    private Duration limiteParcial;

    @Override
    public SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
