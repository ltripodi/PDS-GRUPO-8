package com.skillswap.application;

/*
 * La operacion choco con otra simultanea o duplicada (optimistic locking, clave unica): se hizo
 * rollback completo y no quedo nada a medias. Es un error de aplicacion: la persistencia lo
 * traduce desde la excepcion del proveedor JPA para que ninguna capa superior dependa de JPA.
 * Quien la recibe puede reintentar la operacion.
 */
public class ConflictoConcurrenciaException extends IllegalStateException {

    public ConflictoConcurrenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
