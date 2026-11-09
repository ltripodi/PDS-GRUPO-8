package com.skillswap.infrastructure;

import com.skillswap.application.NotificacionRepository;
import com.skillswap.domain.Notificacion;
import jakarta.persistence.Id;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JpaNotificacionRepository extends SimpleJpaRepository<Notificacion, UUID> implements NotificacionRepository {
    public JpaNotificacionRepository() {
        super(Notificacion.class);
    }

    @Override
    public void guardar(Notificacion notificacion) {
        guardarEntidad(notificacion);
    }

    @Override
    public List<Notificacion> listar(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Notificacion n WHERE n.destinatarioId = :userId ORDER BY n.fecha DESC, n.id", Notificacion.class)
                .setParameter("userId", userId)
                .getResultList());
    }

    @Override
    public Optional<Notificacion> buscarPorId(UUID id) {
        return buscarEntidadPorId(id);
    }
}
