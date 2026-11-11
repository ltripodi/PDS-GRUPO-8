package com.skillswap.controllers;

import com.skillswap.application.DatosPerfilDTO;
import com.skillswap.application.PerfilPublicoDTO;
import com.skillswap.application.PerfilService;

import java.util.UUID;

public class PerfilController {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    public PerfilPublicoDTO verPerfil(String userId) {
        return perfilService.verPerfil(userId);
    }

    public UUID crearPerfil(String userId, DatosPerfilDTO datos) {
        return perfilService.crearPerfil(userId, datos);
    }

    public void actualizarPerfil(String userId, DatosPerfilDTO datos) {
        perfilService.actualizarPerfil(userId, datos);
    }

    public void darDeBaja(String userId) {
        perfilService.darDeBaja(userId);
    }
}
