package com.skillswap.domain.contratacion;

/* RECHAZADA: estado final, solo consultar historial. */
public class EstadoRechazada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "RECHAZADA";
    }
}
