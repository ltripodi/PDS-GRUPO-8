package com.skillswap.domain;

public abstract class EstadoContratacionBase implements EstadoContratacion {
    /*
     * Reconstruye el State a partir del nombre guardado en la DB.
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

    @Override
    public String nombre() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void aceptar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void rechazar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void confirmarPago(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void cancelar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void iniciar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void completar(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void proponerReprogramacion(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void aplicarReprogramacion(Contratacion ctx) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
