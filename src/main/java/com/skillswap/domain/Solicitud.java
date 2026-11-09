package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "solicitudes")
public class Solicitud {
    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(length = 1000)
    private String mensaje;

    private String objetivo;

    @Column(name = "franja_id")
    private UUID franjaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    protected Solicitud() {
    }

    public Solicitud(UUID clienteId, Publicacion publicacion, UUID franjaId, String mensaje, String objetivo) {
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.publicacion = publicacion;
        this.franjaId = franjaId;
        this.mensaje = mensaje;
        this.objetivo = objetivo;
        this.fechaCreacion = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public UUID getFranjaId() {
        return franjaId;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}
