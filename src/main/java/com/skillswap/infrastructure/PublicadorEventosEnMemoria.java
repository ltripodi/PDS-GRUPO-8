package com.skillswap.infrastructure;

import com.skillswap.application.PublicadorEventosPort;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.ObservadorEvento;

import java.util.List;

public class PublicadorEventosEnMemoria implements PublicadorEventosPort {
    private List<ObservadorEvento> observadores;

    @Override
    public void publicarDespuesDelCommit(EventoDominio evento) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public void suscribir(ObservadorEvento obs) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
