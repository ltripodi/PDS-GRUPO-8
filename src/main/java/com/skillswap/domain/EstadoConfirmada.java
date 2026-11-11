package com.skillswap.domain;

/*
 * CONFIRMADA: iniciar, reprogramar o cancelar.
 * El monto del reintegro lo calcula PoliticaCancelacion (Strategy), no el estado.
 */
public class EstadoConfirmada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "CONFIRMADA";
    }

    @Override
    public void iniciar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoEnCurso());
    }

    @Override
    public void cancelar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoCancelada());
    }

    /* Permitido; no cambia el estado. */
    @Override
    public void proponerReprogramacion(Contratacion ctx) {
    }

    /* Permitido; no cambia el estado. */
    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
    }
}
