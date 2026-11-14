package com.skillswap.domain;

import java.util.ArrayList;
import java.util.List;

public class Billetera {
    private String titularId;

    private SkillCoins saldo;

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
