package com.skillswap.domain;

/*
 * State Pattern - clase base de los estados de una Contratacion (consigna 4.5).
 *
 * Por defecto TODAS las acciones son invalidas: cada estado concreto sobrescribe
 * solo las acciones que la tabla de la consigna permite para ese estado.
 * Asi una transicion invalida la rechaza el dominio, sin ifs en Contratacion.
 */
public abstract class EstadoContratacionBase implements EstadoContratacion {

    /*
     * Reconstruye el objeto State a partir del nombre guardado en la DB.
     * Permite persistir el estado como texto y seguir usando polimorfismo en memoria.
     */
    public static EstadoContratacion desdeNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return new EstadoPendiente();
        }

        return switch (nombre) {
            case "PENDIENTE" -> new EstadoPendiente();
            case "ACEPTADA" -> new EstadoAceptada();
            case "CONFIRMADA" -> new EstadoConfirmada();
            case "EN_CURSO" -> new EstadoEnCurso();
            case "COMPLETADA" -> new EstadoCompletada();
            case "RECHAZADA" -> new EstadoRechazada();
            case "CANCELADA" -> new EstadoCancelada();
            default -> throw new IllegalArgumentException("Estado de contratacion desconocido: " + nombre);
        };
    }

    /*
     * Cada estado concreto conoce su propio nombre.
     */
    @Override
    public abstract String nombre();

    @Override
    public void aceptar(Contratacion ctx) {
        throw new IllegalStateException("No se puede aceptar una contratacion en estado " + nombre());
    }

    @Override
    public void rechazar(Contratacion ctx) {
        throw new IllegalStateException("No se puede rechazar una contratacion en estado " + nombre());
    }

    @Override
    public void confirmarPago(Contratacion ctx) {
        throw new IllegalStateException("No se puede pagar una contratacion en estado " + nombre());
    }

    @Override
    public void cancelar(Contratacion ctx) {
        throw new IllegalStateException("No se puede cancelar una contratacion en estado " + nombre());
    }

    @Override
    public void iniciar(Contratacion ctx) {
        throw new IllegalStateException("No se puede iniciar una contratacion en estado " + nombre());
    }

    @Override
    public void completar(Contratacion ctx) {
        throw new IllegalStateException("No se puede completar una contratacion en estado " + nombre());
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx) {
        throw new IllegalStateException("No se puede proponer una reprogramacion en estado " + nombre());
    }

    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
        throw new IllegalStateException("No se puede reprogramar una contratacion en estado " + nombre());
    }

    /*
     * Por defecto ningun estado habilita calificar; solo COMPLETADA lo sobrescribe.
     */
    @Override
    public boolean permiteCalificar() {
        return false;
    }
}
