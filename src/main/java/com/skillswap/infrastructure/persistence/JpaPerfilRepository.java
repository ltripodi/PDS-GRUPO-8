package com.skillswap.infrastructure.persistence;

import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.domain.perfil.Perfil;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    /*
     * Todos los perfiles en una sola consulta.
     * habilidades, intereses y los canales de notificacion son EAGER: sin el JOIN FETCH, Hibernate haria
     * una consulta extra por cada perfil para cargarlos (N+1). Los tres son Set: se pueden traer juntos.
     * disponibilidad ya usa @Fetch(SUBSELECT): se carga con una sola consulta para todos los perfiles.
     *
     * Costo aceptado: traer tres colecciones en el mismo JOIN multiplica las filas (producto cartesiano).
     * Partirlo en tres consultas no sirve aca: como son EAGER, Hibernate las carga perfil por perfil apenas
     * termina la primera consulta, y vuelve el N+1 (medido: de 13 a 29 consultas con 2 y 10 autores).
     * Evitar las dos cosas requiere cambiar el mapeo de Perfil (@Fetch(SUBSELECT) o @BatchSize).
     */
    @Override
    public Map<UUID, Perfil> buscarPorIds(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT p FROM Perfil p "
                        + "LEFT JOIN FETCH p.habilidades "
                        + "LEFT JOIN FETCH p.intereses "
                        + "LEFT JOIN FETCH p.preferenciaNotificacion.canales "
                        + "WHERE p.id IN :ids", Perfil.class)
                .setParameter("ids", ids)
                .getResultList()
                .stream()
                .collect(Collectors.toMap(Perfil::getId, Function.identity(), (a, b) -> a)));
    }
}