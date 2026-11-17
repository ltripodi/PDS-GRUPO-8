package com.skillswap.infrastructure.persistence;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.domain.contratacion.Contratacion;

import jakarta.persistence.Id;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public Pagina<Contratacion> listarPorParticipantePaginado(
            String userId, int pagina, int tamanio) {
        Pagina.validarPedido(pagina, tamanio); // antes de consultar: el tope tiene que cortar antes de la base

        String filtro = " FROM Contratacion c "
                + "WHERE c.clienteId IN "
                + "(SELECT p.id FROM Perfil p WHERE p.userId = :userId) "
                + "OR c.proveedorId IN "
                + "(SELECT p.id FROM Perfil p WHERE p.userId = :userId) "
                + "OR c.proveedorId IN "
                + "(SELECT o.id FROM Organizacion o "
                + "WHERE o.userIdAutorizado = :userId) ";

        return JPAUtil.consultar(em -> {
            long total = em.createQuery(
                            "SELECT COUNT(c)" + filtro, Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();

            int offset = Pagina.desplazamiento(pagina, tamanio);

            if (offset >= total) {
                return new Pagina<>(List.of(), pagina, tamanio, total);
            }

            List<UUID> ids = em.createQuery(
                            "SELECT c.id" + filtro
                                    + "ORDER BY c.solicitud.fechaCreacion DESC, c.id",
                            UUID.class)
                    .setParameter("userId", userId)
                    .setFirstResult(offset)
                    .setMaxResults(tamanio)
                    .getResultList();

            if (ids.isEmpty()) {
                return new Pagina<>(List.of(), pagina, tamanio, total);
            }

            Map<UUID, Contratacion> porId = em.createQuery(
                            "SELECT c FROM Contratacion c WHERE c.id IN :ids",
                            Contratacion.class)
                    .setParameter("ids", ids)
                    .getResultList()
                    .stream()
                    .collect(Collectors.toMap(
                            Contratacion::getId,
                            Function.identity(),
                            (primera, repetida) -> primera
                    ));

            List<Contratacion> ordenadas = ids.stream()
                    .map(porId::get)
                    .toList();

            return new Pagina<>(ordenadas, pagina, tamanio, total);
        });
    }

    @Override
    public void guardar(Contratacion contratacion) {
        guardarEntidad(contratacion);
    }
}
