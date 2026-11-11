package com.skillswap.domain;

/*
 * CANCELADA: estado final. Solo se puede consultar el historial.
 * No sobrescribe ninguna accion: todas las rechaza la clase base.
 */
public class EstadoCancelada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "CANCELADA";
    }
}
