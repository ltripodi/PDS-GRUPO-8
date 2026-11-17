package com.skillswap.presentation.controller;

import com.skillswap.application.compartido.ErrorAplicacion;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Manejo de errores comun a todos los controllers: lo que suba de un caso de uso sale siempre como
 * ErrorAplicacion, nunca como excepcion cruda del dominio o de la persistencia.
 * Los errores inesperados (ERROR_INTERNO) se registran con su causa: el usuario ve un mensaje generico
 * y el detalle tecnico queda en el log para diagnosticar.
 */
abstract class ControllerBase {

    private static final Logger LOG = Logger.getLogger(ControllerBase.class.getName());

    protected <T> T ejecutar(Supplier<T> casoDeUso) {
        try {
            return casoDeUso.get();
        } catch (RuntimeException e) {
            ErrorAplicacion error = ErrorAplicacion.desde(e);
            if (error.getTipo() == ErrorAplicacion.Tipo.ERROR_INTERNO) {
                LOG.log(Level.SEVERE, "Error interno inesperado", e);
            }
            throw error;
        }
    }

    protected void ejecutarAccion(Runnable casoDeUso) {
        ejecutar(() -> {
            casoDeUso.run();
            return null;
        });
    }
}
