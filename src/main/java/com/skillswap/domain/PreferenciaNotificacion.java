package com.skillswap.domain;

import java.util.HashSet;
import java.util.Set;

/** Value Object. */
public class PreferenciaNotificacion {
    private Set<CanalNotificacion> canales;

    protected PreferenciaNotificacion() {
    }

    public PreferenciaNotificacion(Set<CanalNotificacion> canales) {
        this.canales = new HashSet<>(canales);
    }

    public Set<CanalNotificacion> getCanales() {
        return canales;
    }

    public boolean permite(CanalNotificacion canal) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
