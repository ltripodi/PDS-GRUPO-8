package com.skillswap.controllers;

import com.skillswap.application.BusquedaService;
import com.skillswap.application.CategoriaDTO;
import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.Pagina;
import com.skillswap.application.PublicacionDTO;

import java.util.List;

/*
 * Controller de busqueda (4.3): valida la entrada y delega en el caso de uso.
 * Las recomendaciones tienen su propio controller (RecomendacionController).
 * Los listados se devuelven paginados; sin pagina explicita, la primera con el tamanio por defecto.
 */
public class BusquedaController extends ControllerBase {

    private static final int PRIMERA_PAGINA = 0;
    private static final int TAMANIO_POR_DEFECTO = 20;

    private final BusquedaService busquedaService;

    public BusquedaController(BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
    }

    public Pagina<PublicacionDTO> buscar(FiltrosBusqueda filtros) {
        return buscar(filtros, PRIMERA_PAGINA, TAMANIO_POR_DEFECTO);
    }

    public Pagina<PublicacionDTO> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
        return ejecutar(() -> {
            if (filtros == null) {
                throw new IllegalArgumentException("Los filtros de busqueda son obligatorios");
            }
            return busquedaService.buscar(filtros, pagina, tamanio);
        });
    }

    public List<CategoriaDTO> listarCategorias() {
        return ejecutar(busquedaService::listarCategorias);
    }
}
