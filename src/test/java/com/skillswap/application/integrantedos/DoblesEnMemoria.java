package com.skillswap.application.integrantedos;

import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.contratacion.SolicitudRepository;
import com.skillswap.application.notificacion.NotificacionRepository;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.Solicitud;
import com.skillswap.domain.notificacion.Notificacion;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorios en memoria para probar casos de uso sin base de datos.
 * No es una clase de test: la usan {@link NotificacionesTest} y {@link PerfilesYPublicacionesTest}.
 */
final class DoblesEnMemoria {

    private DoblesEnMemoria() {
    }

    static final class Perfiles implements PerfilRepository {
        private final Map<String, Perfil> porUserId = new LinkedHashMap<>();
        private int guardados;

        @Override
        public Optional<Perfil> buscarPorUserId(String userId) {
            return Optional.ofNullable(porUserId.get(userId));
        }

        @Override
        public Optional<Perfil> buscarPorId(UUID id) {
            return porUserId.values().stream().filter(perfil -> perfil.getId().equals(id)).findFirst();
        }

        @Override
        public void guardar(Perfil perfil) {
            porUserId.put(perfil.getUserId(), perfil);
            guardados++;
        }

        /** Cantidad de veces que se llamo a guardar(). */
        int guardados() {
            return guardados;
        }
    }

    static final class Organizaciones implements OrganizacionRepository {
        private final Map<UUID, Organizacion> porId = new HashMap<>();

        @Override
        public Optional<Organizacion> buscarPorId(UUID id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public Optional<Organizacion> buscarPorUserIdAutorizado(String userId) {
            return porId.values().stream().filter(organizacion -> organizacion.autoriza(userId)).findFirst();
        }
    }

    /** Devuelve las notificaciones en el orden en que se guardaron. */
    static final class Notificaciones implements NotificacionRepository {
        private final List<Notificacion> guardadas = new ArrayList<>();

        @Override
        public void guardar(Notificacion notificacion) {
            if (!guardadas.contains(notificacion)) {
                guardadas.add(notificacion);
            }
        }

        @Override
        public Optional<Notificacion> buscarPorId(UUID id) {
            return guardadas.stream().filter(n -> n.getId().equals(id)).findFirst();
        }

        @Override
        public Pagina<Notificacion> listar(String destinatarioId, int pagina, int tamanio) {
            return Pagina.de(de(destinatarioId), pagina, tamanio);
        }

        @Override
        public int marcarTodasLeidas(String destinatarioId) {
            List<Notificacion> noLeidas = de(destinatarioId).stream().filter(n -> !n.isLeida()).toList();
            noLeidas.forEach(Notificacion::marcarLeida);
            return noLeidas.size();
        }

        /* Para verificar en los tests: todas las del destinatario, sin paginar. */
        List<Notificacion> de(String destinatarioId) {
            return guardadas.stream().filter(n -> n.esDe(destinatarioId)).toList();
        }
    }

    static final class Contrataciones implements ContratacionRepository {
        private final Map<UUID, Contratacion> porId = new LinkedHashMap<>();
        private final Perfiles perfiles;

        Contrataciones(Perfiles perfiles) {
            this.perfiles = perfiles;
        }

        @Override
        public Optional<Contratacion> buscarPorId(UUID id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public List<Contratacion> listarPorParticipante(String userId) {
            UUID perfilId = perfiles.buscarPorUserId(userId).map(Perfil::getId).orElse(null);
            return porId.values().stream().filter(c -> c.esParticipante(perfilId)).toList();
        }

        @Override
        public void guardar(Contratacion contratacion) {
            porId.put(contratacion.getId(), contratacion);
        }
    }

    /**
     * buscar() no filtra ni ordena (eso lo hace la consulta JPA y se prueba en PersistenciaTest):
     * solo devuelve las publicadas, paginadas.
     */
    static final class Publicaciones implements PublicacionRepository {
        private final Map<UUID, Publicacion> porId = new LinkedHashMap<>();
        private final Perfiles perfiles;

        /** Con los perfiles, buscarActivasPorAutor pasa de userId a id de Perfil, como la consulta JPA. */
        Publicaciones(Perfiles perfiles) {
            this.perfiles = perfiles;
        }

        @Override
        public void guardar(Publicacion publicacion) {
            porId.put(publicacion.getId(), publicacion);
        }

        @Override
        public Optional<Publicacion> buscarPorId(UUID id) {
            return Optional.ofNullable(porId.get(id));
        }

        @Override
        public List<Publicacion> buscarCandidatas() {
            return porId.values().stream().filter(p -> p.getEstado() == EstadoPublicacion.PUBLICADA).toList();
        }

        @Override
        public List<Publicacion> buscarActivasPorAutor(String userId) {
            UUID perfilId = perfiles.buscarPorUserId(userId).map(Perfil::getId).orElse(null);
            return buscarCandidatas().stream().filter(p -> p.getAutorId().equals(perfilId)).toList();
        }

        @Override
        public List<Publicacion> listarPropias(String userId) {
            return List.of();
        }

        @Override
        public Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
            List<Publicacion> publicadas = buscarCandidatas();
            int desde = Math.min(pagina * tamanio, publicadas.size());
            int hasta = Math.min(desde + tamanio, publicadas.size());
            return new Pagina<>(publicadas.subList(desde, hasta), pagina, tamanio, publicadas.size());
        }

        @Override
        public List<Categoria> listarCategorias() {
            return porId.values().stream().map(Publicacion::getCategoria).distinct().toList();
        }

        @Override
        public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
            return new Pagina<>(List.of(), pagina, tamanio, 0);
        }
    }

    /** Sin solicitudes registradas: la popularidad reciente es cero para todas. */
    static final class Solicitudes implements SolicitudRepository {
        private final Map<UUID, Integer> recientes = new HashMap<>();

        @Override
        public void guardar(Solicitud solicitud) {
            recientes.merge(solicitud.getPublicacion().getId(), 1, Integer::sum);
        }

        @Override
        public Map<UUID, Integer> contarRecientesPorPublicacion(List<UUID> publicacionIds, Instant desde) {
            Map<UUID, Integer> resultado = new HashMap<>();
            for (UUID id : publicacionIds) {
                if (recientes.containsKey(id)) {
                    resultado.put(id, recientes.get(id));
                }
            }
            return resultado;
        }
    }
}