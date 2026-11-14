package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/* Guarda el doble consentimiento; quien es cliente o proveedor lo decide Contratacion. */
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

    @Column(name = "rechazada")
    private boolean rechazada;

    protected PropuestaReprogramacion() {
    }

    public PropuestaReprogramacion(UUID contratacionId, UUID nuevaFranjaId) {
        if (contratacionId == null || nuevaFranjaId == null) {
            throw new IllegalArgumentException("La contratacion y la nueva franja son obligatorias");
        }
        this.id = UUID.randomUUID();
        this.contratacionId = contratacionId;
        this.nuevaFranjaId = nuevaFranjaId;
    }

    public void aceptarComoCliente() {
        validarAbierta();
        this.aceptacionCliente = true;
    }

    public void aceptarComoProveedor() {
        validarAbierta();
        this.aceptacionProveedor = true;
    }

    /* Una parte no acepta: la propuesta se cierra sin mover la reserva. */
    public void rechazar() {
        validarAbierta();
        this.rechazada = true;
    }

    public boolean aceptadaPorAmbos() {
        return aceptacionCliente && aceptacionProveedor;
    }

    public boolean estaPendiente() {
        return !rechazada && !aceptadaPorAmbos();
    }

    private void validarAbierta() {
        if (rechazada) {
            throw new IllegalStateException("La propuesta de reprogramacion ya fue rechazada");
        }
        if (aceptadaPorAmbos()) {
            throw new IllegalStateException("La propuesta de reprogramacion ya fue aceptada por ambas partes");
        }
    }

    public boolean isRechazada() {
        return rechazada;
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
}
