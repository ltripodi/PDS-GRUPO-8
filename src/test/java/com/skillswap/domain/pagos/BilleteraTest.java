package com.skillswap.domain.pagos;

import com.skillswap.domain.compartido.SkillCoins;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BilleteraTest {

    @Test
    void debitar_actualizaSaldoYAgregaMovimiento() {
        Billetera b = new Billetera("user1");
        b.acreditar(new SkillCoins(100));
        b.debitar(new SkillCoins(40));

        assertEquals(60, b.consultarSaldo().getValor());
        assertEquals(2, b.getMovimientos().size());
        assertEquals("CREDITO", b.getMovimientos().get(0).getTipo());
        assertEquals("DEBITO",  b.getMovimientos().get(1).getTipo());
    }

    @Test
    void debitar_saldoInsuficiente_noModificaNada() {
        Billetera b = new Billetera("user1");
        b.acreditar(new SkillCoins(10));
        assertThrows(IllegalStateException.class, () -> b.debitar(new SkillCoins(50)));
        assertEquals(10, b.consultarSaldo().getValor());
        assertEquals(1, b.getMovimientos().size());
    }

    @Test
    void devolver_descuentaYRegistraTipoDevolucion() {
        Billetera b = new Billetera("proveedor");
        b.acreditar(new SkillCoins(50));
        b.devolver(new SkillCoins(30));
        assertEquals(20, b.consultarSaldo().getValor());
        assertEquals("DEVOLUCION", b.getMovimientos().get(1).getTipo());
    }

    @Test
    void devolver_sinSaldoNoModificaNada() {
        Billetera b = new Billetera("proveedor");
        b.acreditar(new SkillCoins(10));
        assertThrows(IllegalStateException.class, () -> b.devolver(new SkillCoins(30)));
        assertEquals(10, b.consultarSaldo().getValor());
        assertEquals(1, b.getMovimientos().size());
    }

    @Test
    void reintegrar_registraTipoReintegro() {
        Billetera b = new Billetera("user1");
        b.reintegrar(new SkillCoins(30));
        assertEquals(30, b.consultarSaldo().getValor());
        assertEquals("REINTEGRO", b.getMovimientos().get(0).getTipo());
    }
}