package com.skillswap.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id
    private UUID id;

    @Column(name = "contratacion_id", nullable = false, unique = true)
    private UUID contratacionId;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "total"))
    private SkillCoins total;

    private Instant fecha;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "total_reintegrado"))
    private SkillCoins totalReintegrado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contratacion_id", insertable = false, updatable = false)
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
