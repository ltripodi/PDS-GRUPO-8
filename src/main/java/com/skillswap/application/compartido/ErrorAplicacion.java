package com.skillswap.application.compartido;

/*
 * Unico tipo de error que los controllers devuelven a la interfaz (consigna, seccion 9: errores
 * de aplicacion manejados de forma uniforme). La interfaz atrapa solo este tipo y decide que mostrar
 * segun getTipo(); el mensaje ya es legible para el usuario.
 */
public class ErrorAplicacion extends RuntimeException {

    public enum Tipo {
        /** Datos de entrada faltantes o incorrectos. */
        DATOS_INVALIDOS,
        /** El usuario no puede realizar la accion sobre ese recurso. */
        SIN_PERMISO,
        /** La accion no es valida en el estado actual (transicion invalida, saldo insuficiente, cupo agotado...). */
        ESTADO_INVALIDO,
        /** Choco con otra operacion simultanea o duplicada; se puede reintentar. */
        CONFLICTO,
        /** Falla inesperada; el detalle queda en la causa, no en el mensaje. */
        ERROR_INTERNO
    }

    private final Tipo tipo;

    public ErrorAplicacion(Tipo tipo, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.tipo = tipo;
    }

    public Tipo getTipo() {
        return tipo;
    }

    /* Clasifica la excepcion que subio de un caso de uso. */
    public static ErrorAplicacion desde(RuntimeException e) {
        if (e instanceof ErrorAplicacion ya) {
            return ya;
        }
        // Orden importa: el conflicto es un IllegalStateException particular.
        if (e instanceof ConflictoConcurrenciaException) {
            return new ErrorAplicacion(Tipo.CONFLICTO, e.getMessage(), e);
        }
        if (e instanceof IllegalArgumentException) {
            return new ErrorAplicacion(Tipo.DATOS_INVALIDOS, e.getMessage(), e);
        }
        if (e instanceof SecurityException) {
            return new ErrorAplicacion(Tipo.SIN_PERMISO, e.getMessage(), e);
        }
        if (e instanceof IllegalStateException) {
            return new ErrorAplicacion(Tipo.ESTADO_INVALIDO, e.getMessage(), e);
        }
        return new ErrorAplicacion(Tipo.ERROR_INTERNO, "Ocurrio un error inesperado. Intente nuevamente.", e);
    }
}
