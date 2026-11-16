package com.skillswap.application;

import java.time.Instant;
import java.util.UUID;

/* Movimiento de billetera como lo ve la interfaz. */
public class MovimientoDTO {
    private final UUID id;
    private final int monto;
    private final String tipo;
    private final Instant fecha;

    public MovimientoDTO(UUID id, int monto, String tipo, Instant fecha) {
        this.id = id;
        this.monto = monto;
        this.tipo = tipo;
        this.fecha = fecha;
    }

    public UUID getId() {
        return id;
    }

    public int getMonto() {
        return monto;
    }

    public String getTipo() {
        return tipo;
    }

    public Instant getFecha() {
        return fecha;
    }
}
