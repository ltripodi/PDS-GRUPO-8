package com.skillswap.domain;

/*
 * ACEPTADA: el proveedor acepto y ya hay una reserva con el cupo bloqueado.
 * Acciones validas (consigna 4.5): pagar, cancelar o proponer reprogramacion.
 */
public class EstadoAceptada extends EstadoContratacionBase {

    @Override
    public String nombre() {
        return "ACEPTADA";
    }

    @Override
    public void confirmarPago(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoConfirmada());
    }

    @Override
    public void cancelar(Contratacion ctx) {
        ctx.cambiarEstado(new EstadoCancelada());
    }

    /*
     * Proponer no cambia el estado: solo habilita la accion.
     * La propuesta en si la crea y guarda la Contratacion.
     */
    @Override
    public void proponerReprogramacion(Contratacion ctx) {
        // Permitido: la contratacion sigue ACEPTADA.
    }

    /*
     * Decision del equipo: si ambas partes aceptan la propuesta antes de pagar,
     * la reserva se mueve igual (la sesion todavia no comenzo).
     */
    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
        // Permitido: la contratacion sigue ACEPTADA, cambia la franja de la reserva.
    }
}
