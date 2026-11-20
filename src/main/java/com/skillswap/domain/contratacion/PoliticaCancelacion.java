package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.SkillCoins;

import java.time.Duration;

/*
 * Strategy Pattern - Strategy
 * Cuanto se devuelve de una contratacion pagada que se cancela. El monto nunca supera el total pagado.
 */
public interface PoliticaCancelacion {
    SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion);
}
