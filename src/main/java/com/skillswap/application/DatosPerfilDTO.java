package com.skillswap.application;

import com.skillswap.domain.DisponibilidadPerfil;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO. */
public class DatosPerfilDTO {
    private final String nombreVisible;
    private final String biografia;
    private final ZoneId zonaHoraria;
    private final Set<UUID> habilidadesIds;
    private final Set<String> intereses;
    private final List<DisponibilidadPerfil> disponibilidad;

    public DatosPerfilDTO(String nombreVisible, String biografia, ZoneId zonaHoraria, Set<UUID> habilidadesIds,
                          Set<String> intereses, List<DisponibilidadPerfil> disponibilidad) {
        this.nombreVisible = nombreVisible;
        this.biografia = biografia;
        this.zonaHoraria = zonaHoraria;
        this.habilidadesIds = habilidadesIds == null ? Set.of() : Set.copyOf(habilidadesIds);
        this.intereses = intereses == null ? Set.of() : Set.copyOf(intereses);
        this.disponibilidad = disponibilidad == null ? List.of() : List.copyOf(disponibilidad);
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public ZoneId getZonaHoraria() {
        return zonaHoraria;
    }

    public Set<UUID> getHabilidadesIds() {
        return habilidadesIds;
    }

    public Set<String> getIntereses() {
        return intereses;
    }

    public List<DisponibilidadPerfil> getDisponibilidad() {
        return disponibilidad;
    }
}
