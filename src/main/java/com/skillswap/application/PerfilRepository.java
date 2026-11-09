package com.skillswap.application;

import com.skillswap.domain.Perfil;

import java.util.Optional;

public interface PerfilRepository {
    Optional<Perfil> buscarPorUserId(String userId);

    void guardar(Perfil perfil);
}
