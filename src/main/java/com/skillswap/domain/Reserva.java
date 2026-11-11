package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.UUID;

/*
 * Reserva de cupo en una FranjaHoraria (consigna 4.4).
 *
 * Crear una reserva OCUPA el cupo; liberarla lo DEVUELVE. Asi el cupo de la franja
 * y la reserva no pueden quedar desincronizados (nadie ocupa un cupo "a mano").
 *
 * La sobreventa concurrente (dos aceptaciones por el ultimo cupo) la frena el
 * @Version de FranjaHoraria (optimistic locking) al hacer commit.
 */
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
        if (franja == null) {
            throw new IllegalArgumentException("La franja es obligatoria");
        }
        if (cantidad <= 0) {
            throw new IllegalArgumentException("La cantidad reservada debe ser positiva");
        }
        if (franja.getOcupados() + cantidad > franja.getCupo()) {
            throw new IllegalStateException("La franja no tiene cupo disponible");
        }

        for (int i = 0; i < cantidad; i++) {
            franja.ocupar();
        }

        this.id = UUID.randomUUID();
        this.franja = franja;
        this.franjaId = franja.getId();
        this.cantidad = cantidad;
        this.vigente = true;
    }

    /*
     * Mueve la reserva a otra franja: primero ocupa la nueva (si no hay cupo, falla
     * sin haber tocado nada) y despues libera la vieja.
     */
    public void reprogramar(FranjaHoraria nueva) {
        validarVigente();
        if (nueva == null) {
            throw new IllegalArgumentException("La nueva franja es obligatoria");
        }
        if (nueva.getId().equals(franjaId)) {
            throw new IllegalArgumentException("La nueva franja debe ser distinta a la actual");
        }
        if (nueva.getOcupados() + cantidad > nueva.getCupo()) {
            throw new IllegalStateException("La nueva franja no tiene cupo disponible");
        }

        for (int i = 0; i < cantidad; i++) {
            nueva.ocupar();
        }
        devolverCupo();

        this.franja = nueva;
        this.franjaId = nueva.getId();
    }

    /*
     * Devuelve el cupo a la franja (por ejemplo, al cancelar). Solo una vez.
     */
    public void liberar() {
        validarVigente();
        devolverCupo();
        this.vigente = false;
    }

    private void devolverCupo() {
        for (int i = 0; i < cantidad; i++) {
            franja.liberar();
        }
    }

    private void validarVigente() {
        if (!vigente) {
            throw new IllegalStateException("La reserva ya fue liberada");
        }
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
}
