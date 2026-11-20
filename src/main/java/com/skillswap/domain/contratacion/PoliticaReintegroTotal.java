package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.SkillCoins;

import java.time.Duration;

/*
 * Strategy Pattern - Concrete Strategy
 * Devuelve todo lo pagado, sin importar la anticipacion. Es la que se aplica cuando cancela el
 * proveedor: el cliente no eligio cancelar, asi que no pierde nada.
 */
public class PoliticaReintegroTotal implements PoliticaCancelacion {

    @Override
    public SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion) {
        if (total == null) {
            throw new IllegalArgumentException("El total es obligatorio");
        }
        return total;
    }
}
