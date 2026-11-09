package com.skillswap.controllers;

import com.skillswap.application.DatosPerfilDTO;
import com.skillswap.application.PerfilService;
import com.skillswap.domain.Perfil;

import java.util.UUID;

public class PerfilController {
    private PerfilService perfilService;

    public Perfil verPerfil(String userId) {
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
}
