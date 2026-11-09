package com.skillswap.controllers;

import com.skillswap.application.BusquedaService;
import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.Pagina;
import com.skillswap.application.RecomendacionService;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.TipoRecomendacion;

import java.util.List;

public class BusquedaController {
    private BusquedaService busquedaService;
    private RecomendacionService recomendacionService;

    public List<Publicacion> buscar(FiltrosBusqueda filtros) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Publicacion> recomendar(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<String> categorias() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Publicacion> buscarPaginado(FiltrosBusqueda filtros, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Publicacion> recomendarPaginado(String userId, TipoRecomendacion tipo, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
