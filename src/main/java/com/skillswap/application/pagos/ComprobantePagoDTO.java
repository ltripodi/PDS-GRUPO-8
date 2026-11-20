package com.skillswap.application.pagos;

import java.time.Instant;
import java.util.UUID;

public class ComprobantePagoDTO {
    private final UUID pagoId;
    private final UUID contratacionId;
    private final int total;
    private final int reintegrado;
    private final Instant fecha;

    public ComprobantePagoDTO(UUID pagoId, UUID contratacionId, int total, int reintegrado, Instant fecha) {
        this.pagoId = pagoId;
        this.contratacionId = contratacionId;
        this.total = total;
        this.reintegrado = reintegrado;
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

    /* Lo que se le devolvio al cliente si la contratacion se cancelo despues de pagar (0 si no). */
    public int getReintegrado() {
        return reintegrado;
    }

    public Instant getFecha() {
        return fecha;
    }
}