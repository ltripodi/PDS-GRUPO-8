package com.skillswap.domain;

public class EstadoEnCurso extends EstadoContratacionBase {
    @Override
    public String nombre() {
        return "EN_CURSO";
    }

    @Override
    public void completar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
