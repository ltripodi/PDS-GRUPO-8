package com.skillswap.application;

import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.ObservadorEvento;

public class NotificacionesObserver implements ObservadorEvento {
    private NotificacionService notificacionService;

    @Override
    public void alOcurrir(EventoDominio evento) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
