package com.skillswap.controllers;

import com.skillswap.application.NotificacionService;
import com.skillswap.application.Pagina;
import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Notificacion;

import java.util.List;
import java.util.Set;

public class NotificacionController {
    private NotificacionService notificacionService;

    public List<Notificacion> listarNotificaciones(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void marcarLeida(String userId, String notificacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void marcarTodasLeidas(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void configurarCanales(String userId, Set<CanalNotificacion> canales) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Notificacion> listarNotificacionesPaginado(String userId, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
