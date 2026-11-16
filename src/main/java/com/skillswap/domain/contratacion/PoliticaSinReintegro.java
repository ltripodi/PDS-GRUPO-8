package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.SkillCoins;

import java.time.Duration;

public class PoliticaSinReintegro implements PoliticaCancelacion {

    @Override
    public SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion) {
        return new SkillCoins(0);
    }
}