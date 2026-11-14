package com.skillswap.application;

import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class BusquedaService {

    private final PublicacionRepository publicaciones;
    private final PerfilRepository perfiles;

    public BusquedaService(PublicacionRepository publicaciones, PerfilRepository perfiles) {
        this.publicaciones = Objects.requireNonNull(publicaciones, "El repositorio de publicaciones es obligatorio");
        this.perfiles = Objects.requireNonNull(perfiles, "El repositorio de perfiles es obligatorio");
    }

    public Pagina<PublicacionDTO> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
        if (filtros == null) {
            throw new IllegalArgumentException("Los filtros de busqueda son obligatorios");
        }
        Pagina.validarPedido(pagina, tamanio);
        return publicaciones.buscar(filtros, pagina, tamanio).convertir(this::convertir);
    }

    /* Catalogo para el filtro de categoria: chico y acotado, se devuelve completo. */
    public List<CategoriaDTO> listarCategorias() {
        return publicaciones.listarCategorias().stream()
                .map(categoria -> new CategoriaDTO(categoria.getId(), categoria.getNombre()))
                .toList();
    }

    /* Los autores de toda la pagina se buscan en una sola consulta (evita el N+1). */
    private List<PublicacionDTO> convertir(List<Publicacion> pagina) {
        Map<UUID, Perfil> autores = perfiles.buscarPorIds(PublicacionMapper.idsDeAutores(pagina));
        return PublicacionMapper.convertir(pagina, autores);
    }
}