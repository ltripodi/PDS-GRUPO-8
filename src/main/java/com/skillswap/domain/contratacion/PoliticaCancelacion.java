package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.SkillCoins;

import java.time.Duration;

public interface PoliticaCancelacion {
    SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion);
}
