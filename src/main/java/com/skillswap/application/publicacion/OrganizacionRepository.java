package com.skillswap.application.publicacion;

import com.skillswap.domain.publicacion.Organizacion;

import java.util.Optional;
import java.util.UUID;

public interface OrganizacionRepository {
    Optional<Organizacion> buscarPorId(UUID id);

    /* La organizacion que el usuario representa (para publicar un desafio sin conocer su id). */
    Optional<Organizacion> buscarPorUserIdAutorizado(String userId);
}
