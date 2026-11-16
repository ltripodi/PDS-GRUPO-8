package com.skillswap.application.notificacion;

import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.ObservadorEvento;

/*
 * Observer Pattern - Concrete Observer
 * Se suscribe al publicador de eventos y, ante cada evento del negocio, delega en NotificacionService.
 * Contratacion y Pagos no lo conocen: solo publican el evento.
 */
public class NotificacionesObserver implements ObservadorEvento {

    private final NotificacionService notificacionService;

    public NotificacionesObserver(NotificacionService notificacionService) {
        if (notificacionService == null) {
            throw new IllegalArgumentException("El servicio de notificaciones es obligatorio");
        }
        this.notificacionService = notificacionService;
    }

    @Override
    public void alOcurrir(EventoDominio evento) {
        notificacionService.notificar(evento);
    }
}
