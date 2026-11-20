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
 * Strategy Pattern - base de las estrategias concretas que asignan un puntaje a cada publicacion.
 * Cada subclase define solo el puntaje; filtrar, ordenar y desempatar es comun a todas.
 */
public abstract class EstrategiaPorPuntaje implements EstrategiaRecomendacion {

    /*
     * A igual puntaje, primero la mas reciente y despues por id. El id no tiene significado de negocio:
     * asegura que el orden sea siempre el mismo, asi la paginacion no repite ni pierde publicaciones.
     */
    private static final Comparator<Publicacion> DESEMPATE = Comparator
            .comparing(Publicacion::getFechaCreacion, Comparator.reverseOrder())
            .thenComparing(Publicacion::getId);

    @Override
    public boolean requiereSolicitudesRecientes() {
        return false;
    }

    @Override
    public final List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas,
                                              Map<UUID, Integer> solicitudesRecientes) {
        validar(perfil, candidatas, solicitudesRecientes);
        return ordenarPorPuntaje(candidatas, publicacion -> puntaje(perfil, publicacion, solicitudesRecientes));
    }

    protected abstract void validar(Perfil perfil, List<Publicacion> candidatas,
                                    Map<UUID, Integer> solicitudesRecientes);

    protected abstract int puntaje(Perfil perfil, Publicacion publicacion, Map<UUID, Integer> solicitudesRecientes);

    /*
     * Deja solo las candidatas con puntaje positivo, de mayor a menor puntaje.
     * El puntaje se calcula una vez por publicacion, no en cada comparacion del ordenamiento.
     */
    private static List<Publicacion> ordenarPorPuntaje(List<Publicacion> candidatas,
                                                       ToIntFunction<Publicacion> puntaje) {
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
