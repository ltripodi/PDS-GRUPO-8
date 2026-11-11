package com.skillswap.application;

import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.Perfil;

import java.util.UUID;

/** Registra calificaciones verificadas y actualiza la reputacion en la misma transaccion. */
public class CalificacionService {
    private final CalificacionRepository calificaciones;
    private final ContratacionRepository contrataciones;
    private final PerfilRepository perfiles;
    private final UnidadTransaccional transacciones;

    public CalificacionService(CalificacionRepository calificaciones, ContratacionRepository contrataciones,
                               PerfilRepository perfiles, UnidadTransaccional transacciones) {
        this.calificaciones = calificaciones;
        this.contrataciones = contrataciones;
        this.perfiles = perfiles;
        this.transacciones = transacciones;
    }

    public void calificar(String userId, UUID contratacionId, String calificadoId, int puntaje) {
        transacciones.ejecutar(() -> {
            Perfil calificador = obtenerPerfil(userId);
            Perfil calificado = obtenerPerfil(calificadoId);
            Contratacion contratacion = contrataciones.buscarPorId(contratacionId)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la contratacion indicada"));

            validarCalificacion(contratacion, calificador, calificado, userId, contratacionId);
            Calificacion calificacion = new Calificacion(contratacionId, userId, calificadoId, puntaje);
            calificaciones.guardar(calificacion);
            calificado.recalcularReputacion(calificaciones.listarVerificadas(calificadoId));
            perfiles.guardar(calificado);
        });
    }

    private void validarCalificacion(Contratacion contratacion, Perfil calificador, Perfil calificado,
                                     String calificadorId, UUID contratacionId) {
        if (!"COMPLETADA".equals(contratacion.getEstado())) {
            throw new IllegalStateException("Solo una contratacion completada puede calificarse");
        }
        if (calificador.getId().equals(calificado.getId())) {
            throw new IllegalArgumentException("Una persona no puede calificarse a si misma");
        }
        boolean actorEsCliente = contratacion.getClienteId().equals(calificador.getId());
        boolean actorEsProveedor = contratacion.getProveedorId().equals(calificador.getId());
        if (!actorEsCliente && !actorEsProveedor) {
            throw new SecurityException("Solo los participantes pueden calificar la contratacion");
        }
        UUID destinatarioEsperado = actorEsCliente ? contratacion.getProveedorId() : contratacion.getClienteId();
        if (!destinatarioEsperado.equals(calificado.getId())) {
            throw new IllegalArgumentException("La calificacion debe dirigirse a la otra parte de la contratacion");
        }
        if (calificaciones.existe(contratacionId, calificadorId)) {
            throw new IllegalStateException("El participante ya califico esta contratacion");
        }
    }

    private Perfil obtenerPerfil(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        return perfiles.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
    }
}
