package com.skillswap.application;

import java.util.UUID;

/* DTO de entrada (command object) para solicitar. */
public class DatosSolicitudDTO {

    private final UUID publicacionId;
    private final UUID franjaId;
    private final String mensaje;
    private final String objetivo;

    public DatosSolicitudDTO(UUID publicacionId, UUID franjaId, String mensaje, String objetivo) {
        this.publicacionId = publicacionId;
        this.franjaId = franjaId;
        this.mensaje = mensaje;
        this.objetivo = objetivo;
    }

    public UUID getPublicacionId() {
        return publicacionId;
    }

    public UUID getFranjaId() {
        return franjaId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }
}
