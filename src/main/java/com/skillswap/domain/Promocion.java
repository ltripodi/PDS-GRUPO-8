package com.skillswap.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "promociones")
public class Promocion {
    @Id
    private UUID id;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "descuento"))
    private SkillCoins descuento;

    protected Promocion() {
    }

    public Promocion(SkillCoins descuento) {
        this.id = UUID.randomUUID();
        this.descuento = descuento;
    }

    public UUID getId() {
        return id;
    }

    public SkillCoins getDescuento() {
        return descuento;
    }

    public SkillCoins aplicar(SkillCoins monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
