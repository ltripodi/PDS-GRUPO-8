package com.skillswap.application.contratacion;

import java.time.LocalDateTime;

/* DTO de salida de una entrada del historial de una contratacion (consigna 4.5 y seccion 5). */
public class RegistroHistorialDTO {

    private final LocalDateTime fecha;
    private final String actor;
    private final String origen;
    private final String destino;
    private final String motivo;

    public RegistroHistorialDTO(LocalDateTime fecha, String actor, String origen, String destino, String motivo) {
        this.fecha = fecha;
        this.actor = actor;
        this.origen = origen;
        this.destino = destino;
        this.motivo = motivo;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public String getActor() {
        return actor;
    }

    public String getOrigen() {
        return origen;
    }

    public String getDestino() {
        return destino;
    }

    public String getMotivo() {
        return motivo;
    }
}
