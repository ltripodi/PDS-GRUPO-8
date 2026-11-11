package com.skillswap.application;

import com.skillswap.domain.Perfil;

import java.util.Optional;
import java.util.UUID;

public interface PerfilRepository {
    Optional<Perfil> buscarPorUserId(String userId);

    Optional<Perfil> buscarPorId(UUID id);

    void guardar(Perfil perfil);
}
