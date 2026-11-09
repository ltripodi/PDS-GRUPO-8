package com.skillswap.infrastructure;

import com.skillswap.application.SolicitudRepository;
import com.skillswap.domain.Solicitud;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JpaSolicitudRepository extends SimpleJpaRepository<Solicitud, UUID> implements SolicitudRepository {
    public JpaSolicitudRepository() {
        super(Solicitud.class);
    }

    @Override
    public void guardar(Solicitud solicitud) {
        guardarEntidad(solicitud);
    }

    @Override
    public Map<UUID, Integer> contarRecientesPorPublicacion(List<UUID> publicacionIds, Instant desde) {
        Map<UUID, Integer> resultado = new HashMap<>();
        if (publicacionIds == null || publicacionIds.isEmpty()) {
            return resultado;
        }

        List<Object[]> filas = JPAUtil.consultar(em -> em
                .createQuery("SELECT s.publicacion.id, COUNT(s) FROM Solicitud s "
                        + "WHERE s.publicacion.id IN :ids AND s.fechaCreacion >= :desde "
                        + "GROUP BY s.publicacion.id", Object[].class)
                .setParameter("ids", publicacionIds)
                .setParameter("desde", desde)
                .getResultList());

        for (Object[] fila : filas) {
            resultado.put((UUID) fila[0], ((Long) fila[1]).intValue());
        }
        return resultado;
    }
}
