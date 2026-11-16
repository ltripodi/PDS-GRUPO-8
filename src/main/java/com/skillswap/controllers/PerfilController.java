package com.skillswap.controllers;

import com.skillswap.application.DatosPerfilDTO;
import com.skillswap.application.PerfilPublicoDTO;
import com.skillswap.application.PerfilService;

import java.util.UUID;

public class PerfilController extends ControllerBase {
    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    public PerfilPublicoDTO verPerfil(String userId) {
        return ejecutar(() -> perfilService.verPerfil(userId));
    }

    public UUID crearPerfil(String userId, DatosPerfilDTO datos) {
        return ejecutar(() -> perfilService.crearPerfil(userId, datos));
    }

    public void actualizarPerfil(String userId, DatosPerfilDTO datos) {
        ejecutarAccion(() -> perfilService.actualizarPerfil(userId, datos));
    }

    public void darDeBaja(String userId) {
        ejecutarAccion(() -> perfilService.darDeBaja(userId));
    }
}
