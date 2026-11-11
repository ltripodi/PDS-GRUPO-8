package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

/*
 * Propuesta de mover una reserva a otra franja (consigna 4.4):
 * "una reserva puede reprogramarse antes de comenzar si ambas partes aceptan".
 *
 * Solo guarda el "doble consentimiento". No sabe quien es cliente o proveedor:
 * eso lo resuelve la Contratacion (Information Expert) y llama al metodo que corresponde.
 */
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

    /*
     * La reprogramacion solo se aplica cuando las dos partes estan de acuerdo.
     */
    public boolean aceptadaPorAmbos() {
        return aceptacionCliente && aceptacionProveedor;
    }

    /*
     * Una propuesta ya aceptada por ambos quedo resuelta: no se puede volver a tocar.
     */
    private void validarAbierta() {
        if (aceptadaPorAmbos()) {
            throw new IllegalStateException("La propuesta de reprogramacion ya fue aceptada por ambas partes");
        }
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
