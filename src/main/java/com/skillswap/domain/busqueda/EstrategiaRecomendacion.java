package com.skillswap.domain.busqueda;

import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Publicacion;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.ToIntFunction;

/*
 * Strategy Pattern - Strategy
 * Cada estrategia define como puntuar una publicacion; ordenar y desempatar es comun a todas.
 */
public interface EstrategiaRecomendacion {

    /*
     * A igual puntaje, primero la mas reciente y despues por id. El id no tiene significado de negocio:
     * asegura que el orden sea siempre el mismo, asi la paginacion no repite ni pierde publicaciones.
     */
    Comparator<Publicacion> DESEMPATE = Comparator
            .comparing(Publicacion::getFechaCreacion, Comparator.reverseOrder())
            .thenComparing(Publicacion::getId);

    TipoRecomendacion tipo();

    List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas, Map<UUID, Integer> solicitudesRecientes);

    /* Si es false, el servicio no consulta las solicitudes recientes y pasa un mapa vacio. */
    default boolean requiereSolicitudesRecientes() {
        return false;
    }

    /*
     * Deja solo las candidatas con puntaje positivo, de mayor a menor puntaje.
     * El puntaje se calcula una vez por publicacion, no en cada comparacion del ordenamiento.
     */
    static List<Publicacion> ordenarPorPuntaje(List<Publicacion> candidatas, ToIntFunction<Publicacion> puntaje) {
        Map<Publicacion, Integer> puntajes = new HashMap<>();
        for (Publicacion candidata : candidatas) {
            int valor = puntaje.applyAsInt(candidata);
            if (valor > 0) {
                puntajes.put(candidata, valor);
            }
        }
        return puntajes.keySet().stream()
                .sorted(Comparator.comparing((Publicacion publicacion) -> puntajes.get(publicacion))
                        .reversed()
                        .thenComparing(DESEMPATE))
                .toList();
    }
}