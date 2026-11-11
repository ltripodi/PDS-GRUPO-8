package com.skillswap.domain;

/*
 * RECHAZADA: estado final. Solo se puede consultar el historial.
 * No sobrescribe ninguna accion: todas las rechaza la clase base.
 */
public class EstadoRechazada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "RECHAZADA";
    }
}
