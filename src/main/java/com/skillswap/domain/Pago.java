package com.skillswap.domain;

import java.time.Instant;
import java.util.UUID;

public class Pago {
    private UUID id;

    private UUID contratacionId;

    private SkillCoins total;

    private Instant fecha;

    private SkillCoins totalReintegrado;

    private Contratacion contratacion;

    protected Pago() {
    }

    public Pago(UUID contratacionId, SkillCoins total) {
        this.id = UUID.randomUUID();
        this.contratacionId = contratacionId;
        this.total = total;
        this.fecha = Instant.now();
        this.totalReintegrado = new SkillCoins(0);
    }

    public UUID getId() {
        return id;
    }

    public UUID getContratacionId() {
        return contratacionId;
    }

    public SkillCoins getTotal() {
        return total;
    }

    public Instant getFecha() {
        return fecha;
    }

    public SkillCoins getTotalReintegrado() {
        return totalReintegrado;
    }

    public Contratacion getContratacion() {
        return contratacion;
    }

    public void registrarReintegro(SkillCoins monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
