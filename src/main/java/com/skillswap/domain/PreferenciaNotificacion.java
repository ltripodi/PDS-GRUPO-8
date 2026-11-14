package com.skillswap.domain;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Value Object. */
public class PreferenciaNotificacion {
    private Set<CanalNotificacion> canales;

    protected PreferenciaNotificacion() {
    }

    public PreferenciaNotificacion(Set<CanalNotificacion> canales) {
        if (canales == null || canales.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Los canales de notificacion no pueden ser nulos");
        }
        if (canales.isEmpty()) {
            throw new IllegalArgumentException("Hay que elegir al menos un canal de notificacion");
        }
        this.canales = new HashSet<>(canales);
    }

    /* Copia de solo lectura: las preferencias se cambian creando otra PreferenciaNotificacion. */
    public Set<CanalNotificacion> getCanales() {
        return canales == null ? Set.of() : Set.copyOf(canales);
    }

    /* canales puede ser null si JPA uso el constructor vacio y no encontro filas. */
    public boolean permite(CanalNotificacion canal) {
        return canal != null && canales != null && canales.contains(canal);
    }

    /* Value Object: dos preferencias son iguales si permiten los mismos canales, no por ser la misma instancia. */
    @Override
    public boolean equals(Object otro) {
        return otro instanceof PreferenciaNotificacion preferencia && getCanales().equals(preferencia.getCanales());
    }

    @Override
    public int hashCode() {
        return getCanales().hashCode();
    }
}
