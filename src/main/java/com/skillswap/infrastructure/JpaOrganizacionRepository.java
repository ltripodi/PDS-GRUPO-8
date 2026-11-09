package com.skillswap.infrastructure;

import com.skillswap.application.OrganizacionRepository;
import com.skillswap.domain.Organizacion;

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
