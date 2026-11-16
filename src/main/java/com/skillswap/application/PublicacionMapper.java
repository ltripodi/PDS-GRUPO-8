package com.skillswap.application;

import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/*
 * Convierte publicaciones en PublicacionDTO. No consulta nada: recibe los autores ya buscados.
 * Un desafio no tiene perfil autor: lo publica una organizacion.
 */
public final class PublicacionMapper {

    private static final String PROVEEDOR_DESCONOCIDO = "Desconocido";

    private PublicacionMapper() {
    }

    /* Ids de los perfiles que hay que buscar para convertir estas publicaciones. */
    public static Set<UUID> idsDeAutores(List<Publicacion> publicaciones) {
        return publicaciones.stream()
                .filter(publicacion -> !esDesafio(publicacion))
                .map(Publicacion::getAutorId)
                .collect(Collectors.toSet());
    }

    public static List<PublicacionDTO> convertir(List<Publicacion> publicaciones, Map<UUID, Perfil> autoresPorId) {
        return publicaciones.stream()
                .map(publicacion -> convertir(publicacion,
                        esDesafio(publicacion) ? null : autoresPorId.get(publicacion.getAutorId())))
                .toList();
    }

    /* autor puede ser null: en un desafio, o si el perfil ya no existe. */
    public static PublicacionDTO convertir(Publicacion publicacion, Perfil autor) {
        return new PublicacionDTO(
                publicacion.getId(),
                publicacion.getTitulo(),
                publicacion.getDescripcion(),
                publicacion.getCategoria().getNombre(),
                publicacion.getModalidad(),
                publicacion.getDuracionMinutos(),
                publicacion.getPrecioBase().getValor(),
                autor != null ? autor.getReputacion() : 0,
                nombreDelProveedor(publicacion, autor),
                publicacion.getEtiquetas().stream().sorted().toList(),
                publicacion.getEstado(),
                publicacion.getFranjas().stream()
                        .sorted(Comparator.comparing(FranjaHoraria::getInicio))
                        .map(f -> new FranjaDTO(f.getId(), f.getInicio(), f.getFin(), f.getCupo() - f.getOcupados()))
                        .toList());
    }

    private static String nombreDelProveedor(Publicacion publicacion, Perfil autor) {
        if (esDesafio(publicacion)) {
            return publicacion.getOrganizacion().getNombre();
        }
        return autor != null ? autor.getNombreVisible() : PROVEEDOR_DESCONOCIDO;
    }

    private static boolean esDesafio(Publicacion publicacion) {
        return publicacion.getOrganizacion() != null;
    }
}