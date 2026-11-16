package com.skillswap.domain.contratacion;

/* EN_CURSO: completar. */
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
