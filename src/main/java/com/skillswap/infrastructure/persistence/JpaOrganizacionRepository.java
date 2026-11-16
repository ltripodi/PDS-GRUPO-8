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

    @Override
    public Optional<Organizacion> buscarPorUserIdAutorizado(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT o FROM Organizacion o WHERE o.userIdAutorizado = :userId", Organizacion.class)
                .setParameter("userId", userId)
                .setMaxResults(1)
                .getResultStream()
                .findFirst());
    }
}
