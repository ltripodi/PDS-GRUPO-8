package com.skillswap.infrastructure;

import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.Pagina;
import com.skillswap.application.PublicacionRepository;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.OrdenPublicacion;
import com.skillswap.domain.Publicacion;
import jakarta.persistence.Id;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class JpaPublicacionRepository extends SimpleJpaRepository<Publicacion, UUID> implements PublicacionRepository {
    public JpaPublicacionRepository() {
        super(Publicacion.class);
    }

    @Override
    public void guardar(Publicacion publicacion) {
        guardarEntidad(publicacion);
    }

    @Override
    public Optional<Publicacion> buscarPorId(UUID id) {
        return buscarEntidadPorId(id);
    }

    @Override
    public List<Publicacion> buscarCandidatas() {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Publicacion p WHERE p.estado = :estado", Publicacion.class)
                .setParameter("estado", EstadoPublicacion.PUBLICADA)
                .getResultList());
    }

    @Override
    public List<Publicacion> buscarActivasPorAutor(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT p FROM Publicacion p WHERE p.perfil.userId = :userId AND p.estado = :estado", Publicacion.class)
                .setParameter("userId", userId)
                .setParameter("estado", EstadoPublicacion.PUBLICADA)
                .getResultList());
    }

    @Override
    public List<Publicacion> listarPropias(String userId) {
        return JPAUtil.consultar(em -> em
                .createQuery("SELECT p FROM Publicacion p "
                        + "LEFT JOIN p.perfil perfil LEFT JOIN p.organizacion organizacion "
                        + "WHERE perfil.userId = :userId OR organizacion.userIdAutorizado = :userId "
                        + "ORDER BY p.fechaCreacion DESC, p.id", Publicacion.class)
                .setParameter("userId", userId)
                .getResultList());
    }

    @Override
    public Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
        // Solo se buscan publicaciones PUBLICADAS. "pagina" es base 0.
        // precioMinimo / precioMaximo / reputacionMinima en 0 significan "sin filtro".
        return JPAUtil.consultar(em -> {
            StringBuilder where = new StringBuilder(" WHERE p.estado = :estado");
            Map<String, Object> params = new HashMap<>();
            params.put("estado", EstadoPublicacion.PUBLICADA);

            if (filtros.getTexto() != null && !filtros.getTexto().isBlank()) {
                where.append(" AND (LOWER(p.titulo) LIKE :texto OR LOWER(p.descripcion) LIKE :texto)");
                params.put("texto", "%" + filtros.getTexto().toLowerCase() + "%");
            }
            if (filtros.getCategoriaId() != null) {
                where.append(" AND p.categoria.id = :categoriaId");
                params.put("categoriaId", filtros.getCategoriaId());
            }
            if (filtros.getModalidad() != null) {
                where.append(" AND p.modalidad = :modalidad");
                params.put("modalidad", filtros.getModalidad());
            }
            if (filtros.getPrecioMinimo() > 0) {
                where.append(" AND p.precioBase.valor >= :precioMinimo");
                params.put("precioMinimo", filtros.getPrecioMinimo());
            }
            if (filtros.getPrecioMaximo() > 0) {
                where.append(" AND p.precioBase.valor <= :precioMaximo");
                params.put("precioMaximo", filtros.getPrecioMaximo());
            }
            if (filtros.getHabilidadId() != null) {
                where.append(" AND EXISTS (SELECT h FROM p.habilidades h WHERE h.id = :habilidadId)");
                params.put("habilidadId", filtros.getHabilidadId());
            }
            if (filtros.getReputacionMinima() > 0) {
                where.append(" AND p.perfil.reputacion >= :reputacionMinima");
                params.put("reputacionMinima", filtros.getReputacionMinima());
            }

            OrdenPublicacion orden = filtros.getOrden() == null ? OrdenPublicacion.RELEVANCIA : filtros.getOrden();
            String order = switch (orden) {
                case REPUTACION -> " ORDER BY p.perfil.reputacion DESC, p.id";
                case MENOR_PRECIO -> " ORDER BY p.precioBase.valor ASC, p.id";
                case MAS_RECIENTE -> " ORDER BY p.fechaCreacion DESC, p.id";
                case RELEVANCIA -> " ORDER BY p.perfil.reputacion DESC, p.fechaCreacion DESC, p.id";
            };

            TypedQuery<Long> count = em.createQuery("SELECT COUNT(p) FROM Publicacion p" + where, Long.class);
            TypedQuery<Publicacion> query = em.createQuery("SELECT p FROM Publicacion p" + where + order, Publicacion.class);
            params.forEach((nombre, valor) -> {
                count.setParameter(nombre, valor);
                query.setParameter(nombre, valor);
            });

            List<Publicacion> elementos = query
                    .setFirstResult(pagina * tamanio)
                    .setMaxResults(tamanio)
                    .getResultList();

            return new Pagina<>(elementos, pagina, tamanio, count.getSingleResult());
        });
    }

    @Override
    public List<Categoria> listarCategorias() {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Categoria c ORDER BY c.nombre", Categoria.class)
                .getResultList());
    }

    @Override
    public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
        return JPAUtil.consultar(em -> {
            Long total = em
                    .createQuery("SELECT COUNT(p) FROM Publicacion p "
                            + "LEFT JOIN p.perfil perfil LEFT JOIN p.organizacion organizacion "
                            + "WHERE perfil.userId = :userId OR organizacion.userIdAutorizado = :userId", Long.class)
                    .setParameter("userId", userId)
                    .getSingleResult();

            List<Publicacion> elementos = em
                    .createQuery("SELECT p FROM Publicacion p "
                            + "LEFT JOIN p.perfil perfil LEFT JOIN p.organizacion organizacion "
                            + "WHERE perfil.userId = :userId OR organizacion.userIdAutorizado = :userId "
                            + "ORDER BY p.fechaCreacion DESC, p.id", Publicacion.class)
                    .setParameter("userId", userId)
                    .setFirstResult(pagina * tamanio)
                    .setMaxResults(tamanio)
                    .getResultList();

            return new Pagina<>(elementos, pagina, tamanio, total);
        });
    }
}
