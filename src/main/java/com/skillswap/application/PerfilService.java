package com.skillswap.application;

import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.Habilidad;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Casos de uso de perfil. La identidad ya fue resuelta por el adapter de entrada. */
public class PerfilService {
    private final PerfilRepository perfilRepository;
    private final HabilidadRepository habilidades;
    private final CalificacionRepository calificaciones;
    private final PublicacionRepository publicaciones;
    private final ContratacionRepository contrataciones;

    public PerfilService(PerfilRepository perfilRepository, HabilidadRepository habilidades,
                         CalificacionRepository calificaciones, PublicacionRepository publicaciones,
                         ContratacionRepository contrataciones) {
        this.perfilRepository = perfilRepository;
        this.habilidades = habilidades;
        this.calificaciones = calificaciones;
        this.publicaciones = publicaciones;
        this.contrataciones = contrataciones;
    }

    public PerfilPublicoDTO verPerfil(String userId) {
        Perfil perfil = obtenerPerfil(userId);
        if (!perfil.isActivo()) {
            throw new IllegalStateException("El perfil no esta disponible publicamente");
        }

        List<Calificacion> resenas = calificaciones.listarVerificadas(userId);
        List<UUID> publicacionesActivas = publicaciones.buscarActivasPorAutor(userId).stream()
                .map(Publicacion::getId)
                .toList();
        List<Contratacion> actividad = contrataciones.listarPorParticipante(userId);
        List<String> historialResumido = actividad.stream()
                .map(contratacion -> "Contratacion " + contratacion.getId() + ": " + contratacion.getEstado())
                .toList();
        int proyectosCompletados = (int) actividad.stream()
                .filter(contratacion -> "COMPLETADA".equals(contratacion.getEstado()))
                .count();
        List<String> habilidadesDelPerfil = perfil.getHabilidades().stream()
                .map(Habilidad::getNombre)
                .sorted()
                .toList();

        return new PerfilPublicoDTO(perfil.getUserId(), perfil.getNombreVisible(), perfil.getBiografia(),
                perfil.obtenerReputacion(), publicacionesActivas, historialResumido, "PROFESIONAL",
                habilidadesDelPerfil, resenas.size(), proyectosCompletados);
    }

    public UUID crearPerfil(String userId, DatosPerfilDTO datos) {
        validarDatos(datos);
        if (perfilRepository.buscarPorUserId(userId).isPresent()) {
            throw new IllegalStateException("Ya existe un perfil para el userId indicado");
        }

        Perfil perfil = new Perfil(userId, datos.getNombreVisible(), datos.getBiografia(), datos.getZonaHoraria());
        aplicarDatosEditables(perfil, datos);
        perfilRepository.guardar(perfil);
        return perfil.getId();
    }

    public void actualizarPerfil(String userId, DatosPerfilDTO datos) {
        validarDatos(datos);
        Perfil perfil = obtenerPerfil(userId);
        aplicarDatosEditables(perfil, datos);
        perfilRepository.guardar(perfil);
    }

    public void darDeBaja(String userId) {
        Perfil perfil = obtenerPerfil(userId);
        perfil.darDeBaja();
        perfilRepository.guardar(perfil);
    }

    public void recalcularReputacion(String userId) {
        Perfil perfil = obtenerPerfil(userId);
        perfil.recalcularReputacion(calificaciones.listarVerificadas(userId));
        perfilRepository.guardar(perfil);
    }

    private void aplicarDatosEditables(Perfil perfil, DatosPerfilDTO datos) {
        Set<Habilidad> habilidadesSolicitadas = resolverHabilidades(datos.getHabilidadesIds());
        perfil.actualizarDatos(datos.getNombreVisible(), datos.getBiografia(), datos.getZonaHoraria());
        perfil.reemplazarHabilidades(habilidadesSolicitadas);
        perfil.actualizarIntereses(datos.getIntereses());
        perfil.definirDisponibilidad(datos.getDisponibilidad());
    }

    private Set<Habilidad> resolverHabilidades(Set<UUID> ids) {
        List<Habilidad> encontradas = habilidades.buscarPorIds(ids);
        if (encontradas.size() != ids.size()) {
            throw new IllegalArgumentException("Se indico una habilidad inexistente");
        }
        return Set.copyOf(encontradas);
    }

    private Perfil obtenerPerfil(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        return perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
    }

    private static void validarDatos(DatosPerfilDTO datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos de perfil son obligatorios");
        }
    }
}
