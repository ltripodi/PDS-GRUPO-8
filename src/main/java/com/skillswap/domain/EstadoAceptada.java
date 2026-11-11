package com.skillswap.domain;

/* ACEPTADA: pagar, cancelar o proponer reprogramacion. */
public class EstadoAceptada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "ACEPTADA";
    }

    @Override
    public void confirmarPago(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoConfirmada());
    }

    @Override
    public void cancelar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoCancelada());
    }

    /* Permitido; no cambia el estado. */
    @Override
    public void proponerReprogramacion(Contratacion ctx) {
    }

    /* Decision del equipo: tambien se puede reprogramar antes de pagar. */
    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
    }
}
