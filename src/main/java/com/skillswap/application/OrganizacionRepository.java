package com.skillswap.application;

import com.skillswap.domain.Organizacion;

import java.util.Optional;
import java.util.UUID;

public interface OrganizacionRepository {
    Optional<Organizacion> buscarPorId(UUID id);
}
