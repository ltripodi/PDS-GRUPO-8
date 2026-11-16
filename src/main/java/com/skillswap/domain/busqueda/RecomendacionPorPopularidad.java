package com.skillswap.domain.busqueda;

import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Publicacion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Strategy Pattern - Concrete Strategy
 * Puntaje: cantidad de solicitudes recientes de la publicacion.
 */
public class RecomendacionPorPopularidad implements EstrategiaRecomendacion {

    @Override
    public TipoRecomendacion tipo() {
        return TipoRecomendacion.POPULARIDAD_RECIENTE;
    }

    @Override
    public boolean requiereSolicitudesRecientes() {
        return true;
    }

    @Override
    public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas,
                                        Map<UUID, Integer> solicitudesRecientes) {
        if (candidatas == null || solicitudesRecientes == null) {
            throw new IllegalArgumentException("Las candidatas y el conteo de solicitudes son obligatorios");
        }
        return EstrategiaRecomendacion.ordenarPorPuntaje(candidatas,
                publicacion -> solicitudesRecientes.getOrDefault(publicacion.getId(), 0));
    }
}