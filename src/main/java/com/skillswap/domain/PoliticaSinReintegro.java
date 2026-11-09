package com.skillswap.domain;

import java.time.Duration;

public class PoliticaSinReintegro implements PoliticaCancelacion {
    @Override
    public SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
