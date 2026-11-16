package com.skillswap.infrastructure.persistence;

import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.domain.publicacion.Organizacion;

import java.util.Optional;
import java.util.UUID;

public class JpaOrganizacionRepository extends SimpleJpaRepository<Organizacion, UUID> implements OrganizacionRepository {
    public JpaOrganizacionRepository() {
        super(Organizacion.class);
    }

    @Override
    public Optional<Organizacion> buscarPorId(UUID id) {
        return buscarEntidadPorId(id);
    }
}
