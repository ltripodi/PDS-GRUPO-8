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
    private PublicacionService publicacionService;

    public List<Publicacion> listarPropias(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Publicacion crearBorrador(String userId, String titulo, String descripcion, String categoria, Modalidad modalidad, int duracion, int precio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Publicacion cambiarEstado(String userId, String publicacionId, EstadoPublicacion estado) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void editarBorrador(String userId, UUID id, DatosPublicacionDTO datos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void agregarFranja(String userId, UUID id, Instant inicio, Instant fin) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cancelar(String userId, UUID id, String motivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
