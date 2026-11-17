package com.skillswap.application.notificacion;

import java.time.LocalDateTime;
import java.util.UUID;

/*
 * Notificacion como la ve la interfaz: solo datos, nunca la entidad.
 * Tiene los mismos campos que Models.Notificacion del kit, incluida la fecha como LocalDateTime
 * (el servicio la pasa a la zona del sistema).
 */
public class NotificacionDTO {

    private final UUID id;
    private final String titulo;
    private final String detalle;
    private final LocalDateTime fecha;
    private final boolean leida;
    private final String tipo;

    public NotificacionDTO(UUID id, String titulo, String detalle, LocalDateTime fecha, boolean leida, String tipo) {
        this.id = id;
        this.titulo = titulo;
        this.detalle = detalle;
        this.fecha = fecha;
        this.leida = leida;
        this.tipo = tipo;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public boolean isLeida() {
        return leida;
    }

    public String getTipo() {
        return tipo;
    }
}