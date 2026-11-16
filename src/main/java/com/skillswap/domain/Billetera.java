package com.skillswap.domain;

import java.util.ArrayList;
import java.util.List;

public class Billetera {
    private String titularId;

    private SkillCoins saldo;

    private List<Movimiento> movimientos = new ArrayList<>();

    /* Optimistic locking (mapeado en orm.xml): dos operaciones concurrentes sobre el mismo saldo no se pisan. */
    private long version;

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

    public long getVersion() {
        return version;
    }

    public List<Movimiento> getMovimientos() {
        return movimientos;
    }

    public void debitar(SkillCoins monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        int nuevo = this.saldo.getValor() - monto.getValor();
        if (nuevo < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }
        this.saldo = new SkillCoins(nuevo);
        this.movimientos.add(new Movimiento(this.titularId, monto, "DEBITO"));
    }

    public void acreditar(SkillCoins monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        this.saldo = new SkillCoins(this.saldo.getValor() + monto.getValor());
        this.movimientos.add(new Movimiento(this.titularId, monto, "CREDITO"));
    }

    public SkillCoins consultarSaldo() {
        return this.saldo;
    }

    /** Reintegro: mismo efecto que acreditar, con tipo propio para auditoría. */
    public void reintegrar(SkillCoins monto) {
        if (monto == null) {
            throw new IllegalArgumentException("El monto es obligatorio");
        }
        this.saldo = new SkillCoins(this.saldo.getValor() + monto.getValor());
        this.movimientos.add(new Movimiento(this.titularId, monto, "REINTEGRO"));
    }
}