package com.skillswap.application;

import java.util.UUID;

public class PerfilService {
    private PerfilRepository perfilRepository;
    private CalificacionRepository calificaciones;
    private PublicacionRepository publicaciones;
    private ContratacionRepository contrataciones;

    public PerfilPublicoDTO verPerfil(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public UUID crearPerfil(String userId, DatosPerfilDTO datos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void actualizarPerfil(String userId, DatosPerfilDTO datos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void darDeBaja(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void recalcularReputacion(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
