package com.skillswap.domain;

public class EstadoAceptada extends EstadoContratacionBase {
    @Override
    public String nombre() {
        return "ACEPTADA";
    }

    @Override
    public void confirmarPago(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void cancelar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
