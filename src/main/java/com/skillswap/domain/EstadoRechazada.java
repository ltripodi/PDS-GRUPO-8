package com.skillswap.domain;

/* RECHAZADA: estado final, solo consultar historial. */
public class EstadoRechazada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "RECHAZADA";
    }
}
