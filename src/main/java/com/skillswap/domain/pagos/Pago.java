package com.skillswap.domain.pagos;

import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;

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
        if (monto == null) {
            throw new IllegalArgumentException("El monto del reintegro es obligatorio");
        }
        int nuevoAcumulado = this.totalReintegrado.getValor() + monto.getValor();
        if (nuevoAcumulado > this.total.getValor()) {
            throw new IllegalStateException("El reintegro supera el total pagado");
        }
        this.totalReintegrado = new SkillCoins(nuevoAcumulado);
    }
}