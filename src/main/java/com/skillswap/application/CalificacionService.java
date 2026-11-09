package com.skillswap.application;

import java.util.UUID;

public class CalificacionService {
    private CalificacionRepository calificaciones;
    private ContratacionRepository contrataciones;
    private PerfilRepository perfiles;
    private UnidadTransaccional transacciones;

    public void calificar(String userId, UUID contratacionId, String calificadoId, int puntaje) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
