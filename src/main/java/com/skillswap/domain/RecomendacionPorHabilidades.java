package com.skillswap.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class RecomendacionPorHabilidades implements EstrategiaRecomendacion {
    @Override
    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas, Map<UUID, Integer> solicitudesRecientes) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public TipoRecomendacion tipo() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
