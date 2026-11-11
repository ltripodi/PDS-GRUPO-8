package com.skillswap.domain;

/* CANCELADA: estado final, solo consultar historial. */
public class EstadoCancelada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "CANCELADA";
    }
}
