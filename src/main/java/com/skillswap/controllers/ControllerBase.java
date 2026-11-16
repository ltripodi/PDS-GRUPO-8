package com.skillswap.controllers;

import com.skillswap.application.ErrorAplicacion;

import java.util.function.Supplier;

/*
 * Manejo de errores comun a todos los controllers: lo que suba de un caso de uso sale siempre como
 * ErrorAplicacion, nunca como excepcion cruda del dominio o de la persistencia.
 */
abstract class ControllerBase {

    protected <T> T ejecutar(Supplier<T> casoDeUso) {
        try {
            return casoDeUso.get();
        } catch (RuntimeException e) {
            throw ErrorAplicacion.desde(e);
        }
    }

    protected void ejecutarAccion(Runnable casoDeUso) {
        try {
            casoDeUso.run();
        } catch (RuntimeException e) {
            throw ErrorAplicacion.desde(e);
        }
    }
}
