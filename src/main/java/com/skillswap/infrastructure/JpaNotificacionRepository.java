package com.skillswap.infrastructure;

import com.skillswap.application.NotificacionRepository;
import com.skillswap.application.Pagina;
import com.skillswap.domain.Notificacion;

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
    public Optional<Notificacion> buscarPorId(UUID id) {
        return buscarEntidadPorId(id);
    }

    /* Pagina en la base: un COUNT y un SELECT con LIMIT/OFFSET, sin traer toda la bandeja. Usa el indice (destinatario, fecha). */
    @Override
    public Pagina<Notificacion> listar(String destinatarioId, int pagina, int tamanio) {
        return JPAUtil.consultar(em -> {
            long total = em.createQuery(
                            "SELECT COUNT(n) FROM Notificacion n WHERE n.destinatarioId = :destinatario", Long.class)
                    .setParameter("destinatario", destinatarioId)
                    .getSingleResult();
            List<Notificacion> elementos = em.createQuery("FROM Notificacion n WHERE n.destinatarioId = :destinatario "
                            + "ORDER BY n.fecha DESC, n.id", Notificacion.class)
                    .setParameter("destinatario", destinatarioId)
                    .setFirstResult(Pagina.desplazamiento(pagina, tamanio))
                    .setMaxResults(tamanio)
                    .getResultList();
            return new Pagina<>(elementos, pagina, tamanio, total);
        });
    }

    /* Un solo UPDATE en una transaccion: o se marcan todas o ninguna, sin cargar la bandeja en memoria. */
    @Override
    public int marcarTodasLeidas(String destinatarioId) {
        return JPAUtil.transaccion(em -> em
                .createQuery("UPDATE Notificacion n SET n.leida = true "
                        + "WHERE n.destinatarioId = :destinatario AND n.leida = false")
                .setParameter("destinatario", destinatarioId)
                .executeUpdate());
    }
}