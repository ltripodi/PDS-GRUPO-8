package com.skillswap.infrastructure;

import com.skillswap.application.CalificacionRepository;
import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Contratacion;
import jakarta.persistence.Id;

import java.util.List;
import java.util.UUID;

public class JpaCalificacionRepository extends SimpleJpaRepository<Calificacion, UUID> implements CalificacionRepository {
    public JpaCalificacionRepository() {
        super(Calificacion.class);
    }

    @Override
    public void guardar(Calificacion calificacion) {
        guardarEntidad(calificacion);
    }

    @Override
    public boolean existe(UUID contratacionId, String calificadorId) {
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT COUNT(c) FROM Calificacion c WHERE c.contratacionId = :contratacionId AND c.calificadorId = :calificadorId", Long.class)
                .setParameter("contratacionId", contratacionId)
                .setParameter("calificadorId", calificadorId)
                .getSingleResult() > 0);
    }

    @Override
    public List<Calificacion> listarVerificadas(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT c FROM Calificacion c WHERE c.calificadoId = :userId "
                        + "AND EXISTS (SELECT k FROM Contratacion k WHERE k.id = c.contratacionId AND k.estado = 'COMPLETADA')", Calificacion.class)
                .setParameter("userId", userId)
                .getResultList());
    }
}
