package com.skillswap.application;

import com.skillswap.domain.Calificacion;

import java.util.List;
import java.util.UUID;

public interface CalificacionRepository {
    void guardar(Calificacion calificacion);

    boolean existe(UUID contratacionId, String calificadorId);

    List<Calificacion> listarVerificadas(String userId);
}
