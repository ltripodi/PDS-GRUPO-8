package com.skillswap.domain;

public interface EstadoContratacion {
    String nombre();

    void aceptar(Contratacion ctx);

    void rechazar(Contratacion ctx);

    void confirmarPago(Contratacion ctx);

    void cancelar(Contratacion ctx);

    void iniciar(Contratacion ctx);

    void completar(Contratacion ctx);

    void proponerReprogramacion(Contratacion ctx);

    void aplicarReprogramacion(Contratacion ctx);

    boolean permiteCalificar();
}
