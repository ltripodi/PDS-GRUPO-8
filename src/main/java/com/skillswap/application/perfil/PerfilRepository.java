package com.skillswap.application.perfil;

import com.skillswap.domain.perfil.Perfil;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PerfilRepository {
    Optional<Perfil> buscarPorUserId(String userId);

    Optional<Perfil> buscarPorId(UUID id);

    void guardar(Perfil perfil);

    default Map<UUID, Perfil> buscarPorIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Perfil> encontrados = new HashMap<>();
        for (UUID id : ids) {
            buscarPorId(id).ifPresent(perfil -> encontrados.put(id, perfil));
        }
        return encontrados;
    }
}