package com.skillswap.presentation.controller;

import com.skillswap.application.calificacion.CalificacionService;

import java.util.UUID;

/* Valida la entrada y delega. Las reglas (completada, otra parte, una sola vez) las aplica el servicio. */
public class CalificacionController extends ControllerBase {
    private final CalificacionService calificacionService;

    public CalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    /*
     * userId: quien califica. calificadoUserId: userId de la otra parte de la contratacion
     * (la UI lo obtiene de ContratacionDTO.getContraparteUserId()).
     */
    public void calificar(String userId, UUID contratacionId, String calificadoUserId, int puntaje) {
        ejecutarAccion(() -> {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("El userId es obligatorio");
            }
            if (contratacionId == null) {
                throw new IllegalArgumentException("La contratacion es obligatoria");
            }
            if (calificadoUserId == null || calificadoUserId.isBlank()) {
                throw new IllegalArgumentException("Hay que indicar a quien se califica");
            }
            if (puntaje < 1 || puntaje > 5) {
                throw new IllegalArgumentException("El puntaje debe estar entre 1 y 5");
            }
            calificacionService.calificar(userId, contratacionId, calificadoUserId, puntaje);
        });
    }
}
