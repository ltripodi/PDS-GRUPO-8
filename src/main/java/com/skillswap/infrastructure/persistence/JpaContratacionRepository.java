package com.skillswap.infrastructure.persistence;

import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.domain.contratacion.Contratacion;

import jakarta.persistence.Id;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaContratacionRepository extends SimpleJpaRepository<Contratacion, UUID> implements ContratacionRepository {
    public JpaContratacionRepository() {
        super(Contratacion.class);
    }

    @Override
    public Optional<Contratacion> buscarPorId(UUID id) {
        return buscarEntidadPorId(id);
    }

    @Override
    public List<Contratacion> listarPorParticipante(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT c FROM Contratacion c "
                        + "WHERE c.clienteId IN (SELECT p.id FROM Perfil p WHERE p.userId = :userId) "
                        + "OR c.proveedorId IN (SELECT p.id FROM Perfil p WHERE p.userId = :userId) "
                        // En un desafio el proveedor es la organizacion: la ve quien la representa.
                        + "OR c.proveedorId IN (SELECT o.id FROM Organizacion o WHERE o.userIdAutorizado = :userId) "
                        + "ORDER BY c.solicitud.fechaCreacion DESC, c.id", Contratacion.class)
                .setParameter("userId", userId)
                .getResultList());
    }

    @Override
    public void guardar(Contratacion contratacion) {
        guardarEntidad(contratacion);
    }
}
