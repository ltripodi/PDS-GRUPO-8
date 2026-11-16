package com.skillswap.controllers;

import com.skillswap.application.DatosPublicacionDTO;
import com.skillswap.application.Pagina;
import com.skillswap.application.PublicacionDTO;
import com.skillswap.application.PublicacionService;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Modalidad;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/* Devuelve siempre PublicacionDTO: la interfaz nunca conoce la entidad Publicacion. */
public class PublicacionController extends ControllerBase {
    private final PublicacionService publicacionService;

    public PublicacionController(PublicacionService publicacionService) {
        this.publicacionService = publicacionService;
    }

    public List<PublicacionDTO> listarPropias(String userId) {
        return ejecutar(() -> publicacionService.aDTOs(publicacionService.listarPropias(userId)));
    }

    /** Compatibilidad con el mock inicial: crea una mentoria individual con cupo uno. */
    public PublicacionDTO crearBorrador(String userId, String titulo, String descripcion, String categoria, Modalidad modalidad, int duracion, int precio) {
        return ejecutar(() -> publicacionService.aDTO(
                publicacionService.crearBorrador(userId, titulo, descripcion, categoria, modalidad, duracion, precio)));
    }

    public PublicacionDTO crearBorrador(String userId, DatosPublicacionDTO datos) {
        return ejecutar(() -> publicacionService.aDTO(publicacionService.crearBorrador(userId, datos)));
    }

    public PublicacionDTO cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado) {
        return ejecutar(() -> {
            if (publicacionId == null || publicacionId.isBlank()) {
                throw new IllegalArgumentException("El id de publicacion es obligatorio");
            }
            return publicacionService.aDTO(
                    publicacionService.cambiarEstado(userId, UUID.fromString(publicacionId), estado));
        });
    }

    public void editarBorrador(String userId, UUID id, DatosPublicacionDTO datos) {
        ejecutarAccion(() -> publicacionService.editarBorrador(userId, id, datos));
    }

    public void agregarFranja(String userId, UUID id, Instant inicio, Instant fin) {
        ejecutarAccion(() -> publicacionService.agregarFranja(userId, id, inicio, fin));
    }

    public void cancelar(String userId, UUID id, String motivo) {
        ejecutarAccion(() -> publicacionService.cancelar(userId, id, motivo));
    }

    public Pagina<PublicacionDTO> listarPropiasPaginado(String userId, int pagina, int tamanio) {
        return ejecutar(() -> publicacionService.listarPropiasPaginado(userId, pagina, tamanio)
                .convertir(publicacionService::aDTOs));
    }
}
