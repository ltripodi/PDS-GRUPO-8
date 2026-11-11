package com.skillswap.application;

import java.util.List;
import java.util.UUID;

/** DTO. */
public class PerfilPublicoDTO {
    private final String userId;
    private final String nombreVisible;
    private final String biografia;
    private final double reputacion;
    private final List<UUID> publicacionesActivasIds;
    private final List<String> historialResumido;
    private final String rol;
    private final List<String> habilidades;
    private final int resenas;
    private final int proyectosCompletados;

    public PerfilPublicoDTO(String userId, String nombreVisible, String biografia, double reputacion,
                            List<UUID> publicacionesActivasIds, List<String> historialResumido, String rol,
                            List<String> habilidades, int resenas, int proyectosCompletados) {
        this.userId = userId;
        this.nombreVisible = nombreVisible;
        this.biografia = biografia;
        this.reputacion = reputacion;
        this.publicacionesActivasIds = List.copyOf(publicacionesActivasIds);
        this.historialResumido = List.copyOf(historialResumido);
        this.rol = rol;
        this.habilidades = List.copyOf(habilidades);
        this.resenas = resenas;
        this.proyectosCompletados = proyectosCompletados;
    }

    public String getUserId() {
        return userId;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public double getReputacion() {
        return reputacion;
    }

    public List<UUID> getPublicacionesActivasIds() {
        return publicacionesActivasIds;
    }

    public List<String> getHistorialResumido() {
        return historialResumido;
    }

    public String getRol() {
        return rol;
    }

    public List<String> getHabilidades() {
        return habilidades;
    }

    public int getResenas() {
        return resenas;
    }

    public int getProyectosCompletados() {
        return proyectosCompletados;
    }
}
