package com.skillswap.domain;

public class EstadoConfirmada extends EstadoContratacionBase {
    @Override
    public String nombre() {
        return "CONFIRMADA";
    }

    @Override
    public void iniciar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void cancelar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
