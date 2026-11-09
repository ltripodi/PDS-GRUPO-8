package com.skillswap.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "billeteras")
public class Billetera {
    @Id
    @Column(name = "titular_id")
    private String titularId;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "saldo"))
    private SkillCoins saldo;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "titular_id", insertable = false, updatable = false)
    private List<Movimiento> movimientos = new ArrayList<>();

    protected Billetera() {
    }

    public Billetera(String titularId) {
        this.titularId = titularId;
        this.saldo = new SkillCoins(0);
    }

    public String getTitularId() {
        return titularId;
    }

    public SkillCoins getSaldo() {
        return saldo;
    }

    public List<Movimiento> getMovimientos() {
        return movimientos;
    }

    public void debitar(SkillCoins monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void acreditar(SkillCoins monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public SkillCoins consultarSaldo() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
