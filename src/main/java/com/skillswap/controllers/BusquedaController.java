package com.skillswap.controllers;

import com.skillswap.application.BusquedaService;
import com.skillswap.application.CategoriaDTO;
import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.Pagina;
import com.skillswap.application.PublicacionDTO;
import com.skillswap.application.RecomendacionService;
import com.skillswap.domain.TipoRecomendacion;

import java.util.List;

/*
 * Controller de 4.3: valida la entrada y delega en los casos de uso.
 * Los listados se devuelven paginados; sin pagina explicita, la primera con el tamanio por defecto.
 */
public class BusquedaController {

    private static final int PRIMERA_PAGINA = 0;
    private static final int TAMANIO_POR_DEFECTO = 20;
    private static final TipoRecomendacion RECOMENDACION_POR_DEFECTO = TipoRecomendacion.HABILIDADES;

    private final BusquedaService busquedaService;
    private final RecomendacionService recomendacionService;

    public BusquedaController(BusquedaService busquedaService, RecomendacionService recomendacionService) {
        this.busquedaService = busquedaService;
        this.recomendacionService = recomendacionService;
    }

    public Pagina<PublicacionDTO> buscar(FiltrosBusqueda filtros) {
        return buscar(filtros, PRIMERA_PAGINA, TAMANIO_POR_DEFECTO);
    }

    public Pagina<PublicacionDTO> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
        if (filtros == null) {
            throw new IllegalArgumentException("Los filtros de busqueda son obligatorios");
        }
        return busquedaService.buscar(filtros, pagina, tamanio);
    }

    public Pagina<PublicacionDTO> recomendar(String userId) {
        return recomendar(userId, RECOMENDACION_POR_DEFECTO, PRIMERA_PAGINA, TAMANIO_POR_DEFECTO);
    }

    public Pagina<PublicacionDTO> recomendar(String userId, TipoRecomendacion tipo, int pagina, int tamanio) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        if (tipo == null) {
            throw new IllegalArgumentException("El tipo de recomendacion es obligatorio");
        }
        return recomendacionService.recomendar(userId, tipo, pagina, tamanio);
    }

    public List<CategoriaDTO> listarCategorias() {
        return busquedaService.listarCategorias();
    }
}