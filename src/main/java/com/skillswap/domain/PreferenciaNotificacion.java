package com.skillswap.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

import java.util.HashSet;
import java.util.Set;

/** Value Object. */
@Embeddable
public class PreferenciaNotificacion {
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_canales_notificacion", joinColumns = @JoinColumn(name = "perfil_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "canal")
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
