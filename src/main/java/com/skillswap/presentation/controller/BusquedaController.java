package com.skillswap.presentation.controller;

import com.skillswap.application.busqueda.BusquedaService;
import com.skillswap.application.busqueda.CategoriaDTO;
import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.publicacion.PublicacionDTO;

import java.util.List;

/*
 * Controller de busqueda (4.3): valida la entrada y delega en el caso de uso.
 * Las recomendaciones tienen su propio controller (RecomendacionController).
 * Los listados se devuelven paginados; sin pagina explicita, la primera con el tamanio por defecto.
 */
public class BusquedaController extends ControllerBase {

    private final BusquedaService busquedaService;

    public BusquedaController(BusquedaService busquedaService) {
        this.busquedaService = busquedaService;
    }

    public Pagina<PublicacionDTO> buscar(FiltrosBusqueda filtros) {
        return buscar(filtros, Pagina.PRIMERA_PAGINA, Pagina.TAMANIO_POR_DEFECTO);
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
