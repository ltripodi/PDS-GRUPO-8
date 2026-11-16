package com.skillswap.domain.contratacion;

/* CANCELADA: estado final, solo consultar historial. */
public class EstadoCancelada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "CANCELADA";
    }
}
