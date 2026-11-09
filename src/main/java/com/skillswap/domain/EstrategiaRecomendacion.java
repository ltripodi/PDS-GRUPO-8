package com.skillswap.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface EstrategiaRecomendacion {
    List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas, Map<UUID, Integer> solicitudesRecientes);

    TipoRecomendacion tipo();
}
