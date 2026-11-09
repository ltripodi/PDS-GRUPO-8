package com.skillswap.application;

import java.util.List;
import java.util.UUID;

/** DTO. */
public class PerfilPublicoDTO {
    private String userId;
    private String nombreVisible;
    private String biografia;
    private double reputacion;
    private List<UUID> publicacionesActivasIds;
    private List<String> historialResumido;
    private String rol;
    private List<String> habilidades;
    private int resenas;
    private int proyectosCompletados;
}
