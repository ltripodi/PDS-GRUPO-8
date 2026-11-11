package com.skillswap.controllers;

import com.skillswap.application.DatosPublicacionDTO;
import com.skillswap.application.Pagina;
import com.skillswap.application.PublicacionService;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.Publicacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PublicacionController {
    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    public List<Publicacion> listarPropias(String userId) {
        return publicacionService.listarPropias(userId);
    }

    /** Compatibilidad con el mock inicial: crea una mentoria individual con cupo uno. */
    public Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria, Modalidad modalidad, int duracion, int precio) {
        return publicacionService.crearBorrador(userId, titulo, descripcion, categoria, modalidad, duracion, precio);
    }

    public Publicacion crearBorrador(String userId, DatosPublicacionDTO datos) {
        return publicacionService.crearBorrador(userId, datos);
    }

    public Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado) {
        if (publicacionId == null || publicacionId.isBlank()) {
            throw new IllegalArgumentException("El id de publicacion es obligatorio");
        }
        return publicacionService.cambiarEstado(userId, UUID.fromString(publicacionId), estado);
    }

    public void editarBorrador(String userId, UUID id, DatosPublicacionDTO datos) {
        publicacionService.editarBorrador(userId, id, datos);
    }

    public void agregarFranja(String userId, UUID id, Instant inicio, Instant fin) {
        publicacionService.agregarFranja(userId, id, inicio, fin);
    }

    public void cancelar(String userId, UUID id, String motivo) {
        publicacionService.cancelar(userId, id, motivo);
    }

    public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
        return publicacionService.listarPropiasPaginado(userId, pagina, tamanio);
    }
}
