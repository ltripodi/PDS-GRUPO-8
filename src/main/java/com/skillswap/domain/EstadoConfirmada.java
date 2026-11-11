package com.skillswap.domain;

/*
 * CONFIRMADA: el cliente pago con SkillCoins.
 * Acciones validas (consigna 4.5): iniciar, reprogramar o cancelar segun politica.
 * El calculo del reintegro (100 / 50 / 0 %) lo resuelve la PoliticaCancelacion (Strategy),
 * no el estado: el estado solo decide SI se puede cancelar.
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

    @Override
    public void proponerReprogramacion(Contratacion ctx) {
        // Permitido: la contratacion sigue CONFIRMADA.
    }

    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
        // Permitido: la contratacion sigue CONFIRMADA, cambia la franja de la reserva.
    }
}
