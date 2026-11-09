package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "propuestas_reprogramacion")
public class PropuestaReprogramacion {
    @Id
    private UUID id;

    @Column(name = "contratacion_id", nullable = false)
    private UUID contratacionId;

    @Column(name = "nueva_franja_id", nullable = false)
    private UUID nuevaFranjaId;

    @Column(name = "aceptacion_cliente")
    private boolean aceptacionCliente;

    @Column(name = "aceptacion_proveedor")
    private boolean aceptacionProveedor;

    protected PropuestaReprogramacion() {
    }

    public PropuestaReprogramacion(UUID contratacionId, UUID nuevaFranjaId) {
        this.id = UUID.randomUUID();
        this.contratacionId = contratacionId;
        this.nuevaFranjaId = nuevaFranjaId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getContratacionId() {
        return contratacionId;
    }

    public UUID getNuevaFranjaId() {
        return nuevaFranjaId;
    }

    public boolean isAceptacionCliente() {
        return aceptacionCliente;
    }

    public boolean isAceptacionProveedor() {
        return aceptacionProveedor;
    }

    public void aceptar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean aceptadaPorAmbos() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
