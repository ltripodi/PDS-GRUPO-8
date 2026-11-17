package com.skillswap.application.calificacion;

import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.domain.calificacion.Calificacion;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Organizacion;

import java.util.UUID;

/** Registra calificaciones verificadas y actualiza la reputacion en la misma transaccion. */
public class CalificacionService {
    private final CalificacionRepository calificaciones;
    private final ContratacionRepository contrataciones;
    private final PerfilRepository perfiles;
    private final OrganizacionRepository organizaciones;
    private final UnidadTransaccional transacciones;

    public CalificacionService(CalificacionRepository calificaciones, ContratacionRepository contrataciones,
                               PerfilRepository perfiles, OrganizacionRepository organizaciones,
                               UnidadTransaccional transacciones) {
        this.calificaciones = calificaciones;
        this.contrataciones = contrataciones;
        this.perfiles = perfiles;
        this.organizaciones = organizaciones;
        this.transacciones = transacciones;
    }

    public void calificar(String userId, UUID contratacionId, String calificadoId, int puntaje) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = contrataciones.buscarPorId(contratacionId)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la contratacion indicada"));
            Participante calificador = resolverParticipante(userId, contratacion);
            Participante calificado = resolverParticipante(calificadoId, contratacion);

            contratacion.validarCalificacion(calificador.id(), calificado.id());
            if (calificaciones.existe(contratacionId, userId)) {
                throw new IllegalStateException("El participante ya califico esta contratacion");
            }
            Calificacion calificacion = new Calificacion(contratacionId, userId, calificadoId, puntaje);
            calificaciones.guardar(calificacion);
            calificado.perfilOpcional().ifPresent(perfil -> {
                perfil.recalcularReputacion(calificaciones.listarVerificadas(calificadoId));
                perfiles.guardar(perfil);
            });
        });
    }

    /* Una organizacion participa mediante su representante; la reputacion sigue siendo propia del Perfil. */
    private Participante resolverParticipante(String userId, Contratacion contratacion) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }

        Perfil perfil = perfiles.buscarPorUserId(userId).orElse(null);
        if (perfil != null && contratacion.esParticipante(perfil.getId())) {
            return new Participante(perfil.getId(), perfil);
        }

        Organizacion organizacion = organizaciones.buscarPorUserIdAutorizado(userId).orElse(null);
        if (organizacion != null && contratacion.esParticipante(organizacion.getId())) {
            return new Participante(organizacion.getId(), null);
        }

        if (perfil == null && organizacion == null) {
            throw new IllegalArgumentException("No existe el participante indicado");
        }
        throw new SecurityException("Solo los participantes pueden calificar la contratacion");
    }

    private record Participante(UUID id, Perfil perfil) {
        java.util.Optional<Perfil> perfilOpcional() {
            return java.util.Optional.ofNullable(perfil);
        }
    }
}
