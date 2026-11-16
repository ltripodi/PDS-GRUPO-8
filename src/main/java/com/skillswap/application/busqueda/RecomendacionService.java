package com.skillswap.application.busqueda;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.contratacion.SolicitudRepository;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.PublicacionDTO;
import com.skillswap.application.publicacion.PublicacionMapper;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.domain.busqueda.EstrategiaRecomendacion;
import com.skillswap.domain.busqueda.TipoRecomendacion;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Publicacion;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/*
 * Strategy Pattern - Context
 * Caso de uso de 4.3: elige la estrategia segun el tipo pedido y delega en ella. Solo conoce la interfaz.
 * Pagina en memoria: el puntaje lo calcula la estrategia, asi que para ordenar hacen falta todas las candidatas.
 */
public class RecomendacionService {

    private final Map<TipoRecomendacion, EstrategiaRecomendacion> estrategias;
    private final PublicacionRepository publicaciones;
    private final PerfilRepository perfiles;
    private final SolicitudRepository solicitudes;
    private final Duration ventanaPopularidad;

    public RecomendacionService(List<EstrategiaRecomendacion> estrategias, PublicacionRepository publicaciones,
                                PerfilRepository perfiles, SolicitudRepository solicitudes,
                                Duration ventanaPopularidad) {
        if (ventanaPopularidad == null || ventanaPopularidad.isZero() || ventanaPopularidad.isNegative()) {
            throw new IllegalArgumentException("La ventana de popularidad debe ser positiva");
        }
        this.estrategias = indexarPorTipo(estrategias);
        this.publicaciones = Objects.requireNonNull(publicaciones, "El repositorio de publicaciones es obligatorio");
        this.perfiles = Objects.requireNonNull(perfiles, "El repositorio de perfiles es obligatorio");
        this.solicitudes = Objects.requireNonNull(solicitudes, "El repositorio de solicitudes es obligatorio");
        this.ventanaPopularidad = ventanaPopularidad;
    }

    public Pagina<PublicacionDTO> recomendar(String userId, TipoRecomendacion tipo, int pagina, int tamanio) {
        Pagina.validarPedido(pagina, tamanio);
        EstrategiaRecomendacion estrategia = estrategiaPara(tipo);
        Perfil perfil = perfiles.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));

        List<Publicacion> candidatas = candidatasPara(perfil);
        List<Publicacion> recomendadas = estrategia.recomendar(perfil, candidatas,
                solicitudesRecientes(estrategia, candidatas));
        return Pagina.de(recomendadas, pagina, tamanio).convertir(this::convertir);
    }

    /* Una sola estrategia por tipo: con dos, cual se usa dependeria del orden de la lista. */
    private static Map<TipoRecomendacion, EstrategiaRecomendacion> indexarPorTipo(
            List<EstrategiaRecomendacion> estrategias) {
        if (estrategias == null || estrategias.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos una estrategia de recomendacion");
        }
        Map<TipoRecomendacion, EstrategiaRecomendacion> porTipo = new EnumMap<>(TipoRecomendacion.class);
        for (EstrategiaRecomendacion estrategia : estrategias) {
            if (estrategia == null || estrategia.tipo() == null) {
                throw new IllegalArgumentException("Cada estrategia debe existir y declarar su tipo");
            }
            if (porTipo.putIfAbsent(estrategia.tipo(), estrategia) != null) {
                throw new IllegalArgumentException("Hay mas de una estrategia para " + estrategia.tipo());
            }
        }
        return porTipo;
    }

    private EstrategiaRecomendacion estrategiaPara(TipoRecomendacion tipo) {
        EstrategiaRecomendacion estrategia = tipo == null ? null : estrategias.get(tipo);
        if (estrategia == null) {
            throw new IllegalArgumentException("No hay una estrategia de recomendacion para " + tipo);
        }
        return estrategia;
    }

    /* Publicadas que no son del propio usuario: ni suyas ni de una organizacion que representa. */
    private List<Publicacion> candidatasPara(Perfil perfil) {
        return publicaciones.buscarCandidatas().stream()
                .filter(publicacion -> !esPropia(publicacion, perfil))
                .toList();
    }

    private boolean esPropia(Publicacion publicacion, Perfil perfil) {
        if (publicacion.getOrganizacion() != null) {
            return publicacion.getOrganizacion().autoriza(perfil.getUserId());
        }
        return publicacion.getAutorId().equals(perfil.getId());
    }

    /* Solo se consultan si la estrategia los usa: la de habilidades no los necesita. */
    private Map<UUID, Integer> solicitudesRecientes(EstrategiaRecomendacion estrategia, List<Publicacion> candidatas) {
        if (!estrategia.requiereSolicitudesRecientes() || candidatas.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = candidatas.stream().map(Publicacion::getId).toList();
        return solicitudes.contarRecientesPorPublicacion(ids, Instant.now().minus(ventanaPopularidad));
    }

    /* Los autores de toda la pagina se buscan en una sola consulta (evita el N+1). */
    private List<PublicacionDTO> convertir(List<Publicacion> pagina) {
        Map<UUID, Perfil> autores = perfiles.buscarPorIds(PublicacionMapper.idsDeAutores(pagina));
        return PublicacionMapper.convertir(pagina, autores);
    }
}