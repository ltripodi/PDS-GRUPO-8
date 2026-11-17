package com.skillswap.infrastructure.persistence;

import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.Publicacion;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JpaPublicacionRepository extends SimpleJpaRepository<Publicacion, UUID> implements PublicacionRepository {

    /* Publicacion con todo lo que leen las estrategias y el DTO. Habilidades y etiquetas son Set: se pueden traer juntas. */
    private static final String SELECT_CON_DETALLE = "SELECT p FROM Publicacion p "
            + "JOIN FETCH p.categoria "
            + "LEFT JOIN FETCH p.organizacion "
            + "LEFT JOIN FETCH p.habilidades "
            + "LEFT JOIN FETCH p.etiquetas ";

    private static final String FROM_BUSQUEDA = " FROM Publicacion p LEFT JOIN p.perfil autor";

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

    /*
     * Fetch plan de la recomendacion: trae en la misma consulta lo que leen las estrategias y el DTO.
     * Sin el JOIN FETCH, Hibernate carga habilidades y etiquetas (EAGER) con una consulta por publicacion (N+1).
     */
    @Override
    public List<Publicacion> buscarCandidatas() {
        return JPAUtil.consultar(em -> em
                .createQuery(SELECT_CON_DETALLE + "WHERE p.estado = :estado", Publicacion.class)
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

    /*
     * Solo publicaciones PUBLICADAS. Fetch plan en dos pasos, para evitar el N+1 sin paginar en memoria:
     * 1) una consulta filtrada, ordenada y paginada devuelve solo los ids de la pagina;
     * 2) otra trae esas publicaciones con JOIN FETCH. No se puede hacer JOIN FETCH de colecciones
     *    en una consulta paginada: Hibernate traeria todo y paginaria en memoria.
     * LEFT JOIN con el perfil autor: los desafios no tienen perfil, no tienen reputacion
     * y en los ordenes por reputacion quedan al final.
     */
    @Override
    public Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
        Pagina.validarPedido(pagina, tamanio); // antes de consultar: el tope tiene que cortar antes de la base
        Map<String, Object> parametros = new HashMap<>();
        String where = condicionesDeBusqueda(filtros, parametros);
        String orderBy = ordenDeBusqueda(filtros);

        return JPAUtil.consultar(em -> {
            long total = crearConsulta(em, "SELECT COUNT(p)" + FROM_BUSQUEDA + where, Long.class, parametros)
                    .getSingleResult();
            List<UUID> ids = crearConsulta(em, "SELECT p.id" + FROM_BUSQUEDA + where + orderBy, UUID.class, parametros)
                    .setFirstResult(Pagina.desplazamiento(pagina, tamanio))
                    .setMaxResults(tamanio)
                    .getResultList();
            return new Pagina<>(buscarEnOrden(em, ids), pagina, tamanio, total);
        });
    }

    private static String condicionesDeBusqueda(FiltrosBusqueda filtros, Map<String, Object> parametros) {
        StringBuilder where = new StringBuilder(" WHERE p.estado = :estado");
        parametros.put("estado", EstadoPublicacion.PUBLICADA);

        if (filtros.getTexto() != null) {
            where.append(" AND (LOWER(p.titulo) LIKE :texto ESCAPE '!' OR LOWER(p.descripcion) LIKE :texto ESCAPE '!')");
            parametros.put("texto", patronContiene(filtros.getTexto()));
        }
        if (filtros.getCategoriaId() != null) {
            where.append(" AND p.categoria.id = :categoriaId");
            parametros.put("categoriaId", filtros.getCategoriaId());
        }
        if (filtros.getModalidad() != null) {
            where.append(" AND p.modalidad = :modalidad");
            parametros.put("modalidad", filtros.getModalidad());
        }
        if (filtros.getPrecioMinimo() != null) {
            where.append(" AND p.precioBase.valor >= :precioMinimo");
            parametros.put("precioMinimo", filtros.getPrecioMinimo());
        }
        if (filtros.getPrecioMaximo() != null) {
            where.append(" AND p.precioBase.valor <= :precioMaximo");
            parametros.put("precioMaximo", filtros.getPrecioMaximo());
        }
        if (filtros.getHabilidadId() != null) {
            where.append(" AND EXISTS (SELECT h FROM p.habilidades h WHERE h.id = :habilidadId)");
            parametros.put("habilidadId", filtros.getHabilidadId());
        }
        if (filtros.getReputacionMinima() != null) {
            where.append(" AND autor.reputacion >= :reputacionMinima");
            parametros.put("reputacionMinima", filtros.getReputacionMinima());
        }
        return where.toString();
    }

    /*
     * RELEVANCIA: si hay texto, primero las que lo tienen en el titulo; despues mejor reputacion y mas reciente.
     * Todos los ordenes terminan en p.id para que la paginacion sea estable.
     */
    private static String ordenDeBusqueda(FiltrosBusqueda filtros) {
        return switch (filtros.getOrden()) {
            case REPUTACION -> " ORDER BY autor.reputacion DESC NULLS LAST, p.id";
            case MENOR_PRECIO -> " ORDER BY p.precioBase.valor ASC, p.id";
            case MAS_RECIENTE -> " ORDER BY p.fechaCreacion DESC, p.id";
            case RELEVANCIA -> " ORDER BY"
                    + (filtros.getTexto() != null ? " CASE WHEN LOWER(p.titulo) LIKE :texto ESCAPE '!' THEN 0 ELSE 1 END," : "")
                    + " autor.reputacion DESC NULLS LAST, p.fechaCreacion DESC, p.id";
        };
    }

    /* "%texto%" en minusculas. %, _ y ! se escapan para que el usuario los busque como texto comun. */
    private static String patronContiene(String texto) {
        String escapado = texto.toLowerCase()
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
        return "%" + escapado + "%";
    }

    private static <R> TypedQuery<R> crearConsulta(EntityManager em, String jpql, Class<R> tipo,
                                                   Map<String, Object> parametros) {
        TypedQuery<R> consulta = em.createQuery(jpql, tipo);
        parametros.forEach(consulta::setParameter);
        return consulta;
    }

    /* Segundo paso de buscar: una sola consulta para toda la pagina, respetando el orden de los ids. */
    private static List<Publicacion> buscarEnOrden(EntityManager em, List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Publicacion> porId = em.createQuery(SELECT_CON_DETALLE + "WHERE p.id IN :ids", Publicacion.class)
                .setParameter("ids", ids)
                .getResultList().stream()
                .collect(Collectors.toMap(Publicacion::getId, Function.identity(), (repetida, otra) -> repetida));
        return ids.stream().map(porId::get).toList();
    }

    @Override
    public List<Categoria> listarCategorias() {
        return JPAUtil.consultar(em -> em
                .createQuery("FROM Categoria c ORDER BY c.nombre", Categoria.class)
                .getResultList());
    }

    @Override
    public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
        Pagina.validarPedido(pagina, tamanio); // antes de consultar: el tope tiene que cortar antes de la base
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
                    .setFirstResult(Pagina.desplazamiento(pagina, tamanio))
                    .setMaxResults(tamanio)
                    .getResultList();

            return new Pagina<>(elementos, pagina, tamanio, total);
        });
    }
}