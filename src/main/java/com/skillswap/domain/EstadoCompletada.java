package com.skillswap.domain;

/*
 * COMPLETADA: estado final.
 * Acciones validas (consigna 4.5): calificar y consultar comprobante.
 * Ninguna de las dos es una transicion, por eso no sobrescribe nada:
 * cualquier intento de cambiar el estado lo rechaza la clase base.
 */
public class EstadoCompletada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "COMPLETADA";
    }

    /*
     * Unico estado que habilita calificar (lo consulta Contratacion.habilitaCalificacion()).
     */
    @Override
    public boolean permiteCalificar() {
        return true;
    }
}
