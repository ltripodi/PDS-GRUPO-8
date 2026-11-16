package com.skillswap.domain.contratacion;

/* PENDIENTE: aceptar o rechazar. */
public class EstadoPendiente extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "PENDIENTE";
    }

    @Override
    public void aceptar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoAceptada());
    }

    @Override
    public void rechazar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoRechazada());
    }
}
