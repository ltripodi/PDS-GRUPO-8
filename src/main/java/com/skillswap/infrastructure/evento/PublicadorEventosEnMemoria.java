package com.skillswap.infrastructure.evento;

import com.skillswap.application.evento.PublicadorEventosPort;
import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.ObservadorEvento;
import com.skillswap.infrastructure.persistence.JPAUtil;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Observer Pattern - Subject
 * Guarda los observadores y les entrega cada evento cuando la transaccion se confirma.
 * Si la transaccion hace rollback, el evento se descarta (ver JPAUtil.despuesDelCommit).
 */
public class PublicadorEventosEnMemoria implements PublicadorEventosPort {

    private static final Logger LOG = Logger.getLogger(PublicadorEventosEnMemoria.class.getName());

    private final CopyOnWriteArrayList<ObservadorEvento> observadores = new CopyOnWriteArrayList<>();

    @Override
    public void publicarDespuesDelCommit(EventoDominio evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El evento es obligatorio");
        }
        JPAUtil.despuesDelCommit(() -> avisarATodos(evento));
    }

    /* El mismo observador suscripto dos veces recibiria cada evento dos veces: se ignora el repetido. */
    @Override
    public void suscribir(ObservadorEvento obs) {
        if (obs == null) {
            throw new IllegalArgumentException("El observador es obligatorio");
        }
        observadores.addIfAbsent(obs); // atomico: dos hilos no pueden suscribir el mismo observador dos veces
    }

    /* Un observador que falla no impide que los demas se enteren. Se recorre una foto de la lista. */
    private void avisarATodos(EventoDominio evento) {
        for (ObservadorEvento observador : observadores) {
            try {
                observador.alOcurrir(evento);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Fallo un observador al procesar " + evento.getTipo(), e);
            }
        }
    }
}
