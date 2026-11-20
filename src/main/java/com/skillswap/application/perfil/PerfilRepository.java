package com.skillswap.application.perfil;

import com.skillswap.domain.perfil.Perfil;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PerfilRepository {
    Optional<Perfil> buscarPorUserId(String userId);

    Optional<Perfil> buscarPorId(UUID id);

    /* En lote, para no hacer una consulta por autor al armar una pagina. Sin ids devuelve un mapa vacio. */
    Map<UUID, Perfil> buscarPorIds(Set<UUID> ids);

    void guardar(Perfil perfil);
}
