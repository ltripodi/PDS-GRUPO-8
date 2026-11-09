package com.skillswap.application;

import com.skillswap.domain.Categoria;
import com.skillswap.domain.Publicacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicacionRepository {
    void guardar(Publicacion publicacion);

    Optional<Publicacion> buscarPorId(UUID id);

    List<Publicacion> buscarCandidatas();

    List<Publicacion> buscarActivasPorAutor(String userId);

    Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio);

    List<Categoria> listarCategorias();

    Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio);
}
