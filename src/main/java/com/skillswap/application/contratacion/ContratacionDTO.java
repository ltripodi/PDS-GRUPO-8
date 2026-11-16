package com.skillswap.application.contratacion;

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
    /* userId de la otra parte: lo necesita la UI para calificarla. */
    private final String contraparteUserId;

    public ContratacionDTO(UUID id, String titulo, String contraparte, LocalDateTime fecha,
                           int precio, EstadoContratacionDTO estado) {
        this(id, titulo, contraparte, null, fecha, precio, estado);
    }

    public ContratacionDTO(UUID id, String titulo, String contraparte, String contraparteUserId,
                           LocalDateTime fecha, int precio, EstadoContratacionDTO estado) {
        this.contraparteUserId = contraparteUserId;
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

    public String getContraparteUserId() {
        return contraparteUserId;
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
