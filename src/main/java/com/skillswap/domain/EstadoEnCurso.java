package com.skillswap.domain;

/*
 * EN_CURSO: la sesion comenzo.
 * Accion valida (consigna 4.5): completar.
 * Ya no se puede cancelar ni reprogramar (la reserva "comenzo").
 */
public class EstadoEnCurso extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "EN_CURSO";
    }

    @Override
    public void completar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoCompletada());
    }
}
