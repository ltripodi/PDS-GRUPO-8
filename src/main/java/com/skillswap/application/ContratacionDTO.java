package com.skillswap.application;

import java.time.LocalDateTime;
import java.util.UUID;

/* DTO de salida: la UI nunca recibe la entidad Contratacion. */
public class ContratacionDTO {

    private final UUID id;
    private final String titulo;
    private final String contraparte;
    private final LocalDateTime fecha;
    private final int precio;
    private final EstadoContratacionDTO estado;

    public ContratacionDTO(UUID id, String titulo, String contraparte, LocalDateTime fecha,
                           int precio, EstadoContratacionDTO estado) {
        this.id = id;
        this.titulo = titulo;
        this.contraparte = contraparte;
        this.fecha = fecha;
        this.precio = precio;
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getContraparte() {
        return contraparte;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public int getPrecio() {
        return precio;
    }

    public EstadoContratacionDTO getEstado() {
        return estado;
    }
}
