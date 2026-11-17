package com.skillswap.application.pagos;

import java.time.Instant;
import java.util.UUID;

public class ComprobantePagoDTO {
    private final UUID pagoId;
    private final UUID contratacionId;
    private final int total;
    private final Instant fecha;

    public ComprobantePagoDTO(UUID pagoId, UUID contratacionId, int total, Instant fecha) {
        this.pagoId = pagoId;
        this.contratacionId = contratacionId;
        this.total = total;
        this.fecha = fecha;
    }

    public UUID getPagoId() {
        return pagoId;
    }

    public UUID getContratacionId() {
        return contratacionId;
    }

    public int getTotal() {
        return total;
    }

    public Instant getFecha() {
        return fecha;
    }
}