package com.skillswap.domain.busqueda;

import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Publicacion;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 * Strategy Pattern - Strategy
 * Solo el contrato: lo comun a las estrategias que puntuan (ordenar y desempatar) esta en
 * EstrategiaPorPuntaje.
 */
public interface EstrategiaRecomendacion {

    TipoRecomendacion tipo();

    List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas, Map<UUID, Integer> solicitudesRecientes);

    /* Si es false, el servicio no consulta las solicitudes recientes y pasa un mapa vacio. */
    boolean requiereSolicitudesRecientes();
}
