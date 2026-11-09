package com.skillswap.domain;

public class EstadoPendiente extends EstadoContratacionBase {
    @Override
    public String nombre() {
        return "PENDIENTE";
    }

    @Override
    public void aceptar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void rechazar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
