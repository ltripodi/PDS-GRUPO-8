package com.skillswap.application.calificacion;

import com.skillswap.domain.calificacion.Calificacion;

import java.util.List;
import java.util.UUID;

public interface CalificacionRepository {
    void guardar(Calificacion calificacion);

    boolean existe(UUID contratacionId, String calificadorId);

    List<Calificacion> listarVerificadas(String userId);
}
