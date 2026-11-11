package com.skillswap.infrastructure;

import com.skillswap.application.PerfilRepository;
import com.skillswap.domain.Perfil;
import jakarta.persistence.Id;

import java.util.Optional;
import java.util.UUID;

public class JpaPerfilRepository extends SimpleJpaRepository<Perfil, UUID> implements PerfilRepository {
    public JpaPerfilRepository() {
        super(Perfil.class);
    }

    @Override
    public Optional<Perfil> buscarPorUserId(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Perfil p WHERE p.userId = :userId", Perfil.class)
                .setParameter("userId", userId)
                .getResultList()
                .stream()
                .findFirst());
    }

    @Override
    public Optional<Perfil> buscarPorId(UUID id) {
        return buscarEntidadPorId(id);
    }

    @Override
    public void guardar(Perfil perfil) {
        guardarEntidad(perfil);
    }
}
