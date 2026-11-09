package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "reservas")
public class Reserva {
    @Id
    private UUID id;

    @Column(name = "franja_id", nullable = false)
    private UUID franjaId;

    private int cantidad;

    private boolean vigente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "franja_id", insertable = false, updatable = false)
    private FranjaHoraria franja;

    protected Reserva() {
    }

    public Reserva(FranjaHoraria franja, int cantidad) {
        this.id = UUID.randomUUID();
        this.franja = franja;
        this.franjaId = franja.getId();
        this.cantidad = cantidad;
        this.vigente = true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getFranjaId() {
        return franjaId;
    }

    public int getCantidad() {
        return cantidad;
    }

    public boolean isVigente() {
        return vigente;
    }

    public FranjaHoraria getFranja() {
        return franja;
    }

    public void reprogramar(FranjaHoraria nueva) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void liberar() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
