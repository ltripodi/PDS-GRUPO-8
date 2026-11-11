package com.skillswap.domain;

/*
 * State Pattern - base: todas las acciones son invalidas por defecto.
 * Cada estado sobrescribe solo las que permite la tabla 4.5.
 */
public abstract class EstadoContratacionBase implements EstadoContratacion {

    /* Reconstruye el State a partir del nombre guardado en la DB. */
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

    /* Solo COMPLETADA habilita calificar. */
    @Override
    public boolean permiteCalificar() {
        return false;
    }
}
