package com.skillswap.domain;

import java.time.Duration;

public interface PoliticaCancelacion {
    SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion);
}
