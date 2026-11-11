package com.skillswap.domain;

/* COMPLETADA: estado final. Calificar y consultar comprobante (no son transiciones). */
public class EstadoCompletada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "COMPLETADA";
    }

    @Override
    public boolean permiteCalificar() {
        return true;
    }
}
