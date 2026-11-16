package com.skillswap.application.publicacion;

import com.skillswap.domain.publicacion.Organizacion;

import java.util.Optional;
import java.util.UUID;

public interface OrganizacionRepository {
    Optional<Organizacion> buscarPorId(UUID id);
}
