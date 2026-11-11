package com.skillswap.infrastructure;

import com.skillswap.application.HabilidadRepository;
import com.skillswap.domain.Habilidad;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JpaHabilidadRepository extends SimpleJpaRepository<Habilidad, UUID> implements HabilidadRepository {
    public JpaHabilidadRepository() {
        super(Habilidad.class);
    }

    @Override
    public List<Habilidad> buscarPorIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Habilidad h WHERE h.id IN :ids", Habilidad.class)
                .setParameter("ids", ids)
                .getResultList());
    }
}
