package com.skillswap.application.compartido;

import com.skillswap.application.compartido.ErrorAplicacion.Tipo;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/* Clasificacion uniforme de errores: la interfaz solo conoce ErrorAplicacion y su tipo. */
class ErrorAplicacionTest {

    @Test
    void cadaExcepcionDeNegocioTieneSuTipo() {
        assertEquals(Tipo.DATOS_INVALIDOS, ErrorAplicacion.desde(new IllegalArgumentException("x")).getTipo());
        assertEquals(Tipo.SIN_PERMISO, ErrorAplicacion.desde(new SecurityException("x")).getTipo());
        assertEquals(Tipo.ESTADO_INVALIDO, ErrorAplicacion.desde(new IllegalStateException("x")).getTipo());
    }

    @Test
    void unConflictoDeConcurrenciaNoSeConfundeConUnEstadoInvalido() {
        // ConflictoConcurrenciaException ES un IllegalStateException: el orden de la clasificacion importa.
        ErrorAplicacion error = ErrorAplicacion.desde(new ConflictoConcurrenciaException("choco", null));

        assertEquals(Tipo.CONFLICTO, error.getTipo());
        assertEquals("choco", error.getMessage());
    }

    @Test
    void unaFallaInesperadaNoFiltraSuDetalleAlMensaje() {
        ErrorAplicacion error = ErrorAplicacion.desde(new NullPointerException("detalle interno"));

        assertEquals(Tipo.ERROR_INTERNO, error.getTipo());
        assertEquals("Ocurrio un error inesperado. Intente nuevamente.", error.getMessage());
        assertEquals("detalle interno", error.getCause().getMessage(), "el detalle queda en la causa");
    }

    @Test
    void unErrorYaClasificadoNoSeEnvuelveDeNuevo() {
        ErrorAplicacion original = new ErrorAplicacion(Tipo.SIN_PERMISO, "no", null);

        assertSame(original, ErrorAplicacion.desde(original));
    }
}
