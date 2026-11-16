package com.skillswap.domain;

import java.time.Duration;

public class PoliticaPorAnticipacion implements PoliticaCancelacion {

    private final Duration limiteTotal;    // >= este → 100%
    private final Duration limiteParcial;  // >= este → 50%, si no 0%

    public PoliticaPorAnticipacion(Duration limiteTotal, Duration limiteParcial) {
        if (limiteTotal == null || limiteParcial == null) {
            throw new IllegalArgumentException("Los límites son obligatorios");
        }
        if (limiteParcial.compareTo(limiteTotal) > 0) {
            throw new IllegalArgumentException("limiteParcial no puede ser mayor que limiteTotal");
        }
        this.limiteTotal = limiteTotal;
        this.limiteParcial = limiteParcial;
    }

    @Override
    public SkillCoins calcularReintegro(SkillCoins total, Duration anticipacion) {
        if (total == null || anticipacion == null) {
            throw new IllegalArgumentException("Total y anticipación son obligatorios");
        }
        int porcentaje;
        if (anticipacion.compareTo(limiteTotal) >= 0) {
            porcentaje = 100;
        } else if (anticipacion.compareTo(limiteParcial) >= 0) {
            porcentaje = 50;
        } else {
            porcentaje = 0;
        }
        int monto = total.getValor() * porcentaje / 100;
        if (monto > total.getValor()) {       // clamp defensivo
            monto = total.getValor();
        }
        return new SkillCoins(monto);
    }
}