package com.skillswap.application;

import com.skillswap.domain.Habilidad;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Puerto de consulta de habilidades para los comandos de perfil. */
public interface HabilidadRepository {
    List<Habilidad> buscarPorIds(Set<UUID> ids);
}
