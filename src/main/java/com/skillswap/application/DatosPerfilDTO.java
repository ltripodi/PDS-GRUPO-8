package com.skillswap.application;

import com.skillswap.domain.DisponibilidadPerfil;

import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** DTO. */
public class DatosPerfilDTO {
    private String nombreVisible;
    private String biografia;
    private ZoneId zonaHoraria;
    private Set<UUID> habilidadesIds;
    private Set<String> intereses;
    private List<DisponibilidadPerfil> disponibilidad;
}
