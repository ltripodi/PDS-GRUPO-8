package com.skillswap.domain.busqueda;

import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Publicacion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Strategy Pattern - Concrete Strategy
 * Puntaje: cantidad de habilidades que la publicacion comparte con el perfil.
 */
public class RecomendacionPorHabilidades extends EstrategiaPorPuntaje {

    @Override
    public TipoRecomendacion tipo() {
        return TipoRecomendacion.HABILIDADES;
    }

    @Override
    protected void validar(Perfil perfil, List<Publicacion> candidatas, Map<UUID, Integer> solicitudesRecientes) {
        if (perfil == null || candidatas == null) {
            throw new IllegalArgumentException("El perfil y las candidatas son obligatorios");
        }
    }

    @Override
    protected int puntaje(Perfil perfil, Publicacion publicacion, Map<UUID, Integer> solicitudesRecientes) {
        return (int) publicacion.getHabilidades().stream()
                .filter(perfil.getHabilidades()::contains)
                .count();
    }
}
