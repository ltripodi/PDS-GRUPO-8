package com.skillswap.infrastructure;

import com.skillswap.application.ContratacionRepository;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.Perfil;
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
                        + "ORDER BY c.solicitud.fechaCreacion DESC, c.id", Contratacion.class)
                .setParameter("userId", userId)
                .getResultList());
    }

    @Override
    public void guardar(Contratacion contratacion) {
        guardarEntidad(contratacion);
    }
}
