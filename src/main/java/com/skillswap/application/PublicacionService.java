package com.skillswap.application;

import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Publicacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PublicacionService {
    private PublicacionRepository publicacionRepository;
    private PerfilRepository perfilRepository;
    private OrganizacionRepository organizacionRepository;

    public List<Publicacion> listarPropias(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Publicacion crearBorrador(String userId, DatosPublicacionDTO datos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void editarBorrador(String userId, UUID id, DatosPublicacionDTO datos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cambiarEstado(String userId, UUID id, EstadoPublicacion estado) {
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

    private void validarPermiso(String userId, Publicacion publicacion) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
