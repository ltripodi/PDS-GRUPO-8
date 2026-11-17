package com.skillswap.presentation.controller;

import com.skillswap.application.compartido.ErrorAplicacion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Todo error sale como ErrorAplicacion. Solo los inesperados (ERROR_INTERNO) se registran en el log con su causa:
 * los de negocio son parte del flujo normal y no son fallas a diagnosticar.
 */
class ControllerBaseTest {

    private final Logger log = Logger.getLogger(ControllerBase.class.getName());
    private final List<LogRecord> registrados = new ArrayList<>();
    private final Handler captura = new Handler() {
        @Override
        public void publish(LogRecord registro) {
            registrados.add(registro);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    };
    private boolean usabaHandlersDelPadre;

    private final ControllerBase controller = new ControllerBase() {
    };

    @BeforeEach
    void capturarLog() {
        usabaHandlersDelPadre = log.getUseParentHandlers();
        log.setUseParentHandlers(false); // que el error esperado no ensucie la salida de los tests
        log.addHandler(captura);
    }

    @AfterEach
    void soltarLog() {
        log.removeHandler(captura);
        log.setUseParentHandlers(usabaHandlersDelPadre);
    }

    @Test
    void unErrorInternoSeRegistraConSuCausa() {
        NullPointerException falla = new NullPointerException("detalle interno");

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class, () -> controller.ejecutar(() -> {
            throw falla;
        }));

        assertEquals(ErrorAplicacion.Tipo.ERROR_INTERNO, error.getTipo());
        assertEquals(1, registrados.size());
        assertEquals(Level.SEVERE, registrados.get(0).getLevel());
        assertSame(falla, registrados.get(0).getThrown(), "el log guarda la excepcion original");
    }

    @Test
    void lasAccionesSinResultadoTambienRegistranElErrorInterno() {
        assertThrows(ErrorAplicacion.class, () -> controller.ejecutarAccion(() -> {
            throw new UnsupportedOperationException("falla");
        }));

        assertEquals(1, registrados.size());
    }

    @Test
    void losErroresDeNegocioNoSeRegistran() {
        assertThrows(ErrorAplicacion.class, () -> controller.ejecutar(() -> {
            throw new IllegalArgumentException("dato invalido");
        }));
        assertThrows(ErrorAplicacion.class, () -> controller.ejecutarAccion(() -> {
            throw new IllegalStateException("transicion invalida");
        }));
        assertThrows(ErrorAplicacion.class, () -> controller.ejecutar(() -> {
            throw new SecurityException("sin permiso");
        }));

        assertTrue(registrados.isEmpty());
    }

    @Test
    void sinErrorDevuelveElResultado() {
        assertEquals(7, controller.ejecutar(() -> 7));
        assertTrue(registrados.isEmpty());
    }
}
