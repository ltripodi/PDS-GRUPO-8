package com.skillswap.infrastructure.adapters.notificacion;

import com.skillswap.application.notificacion.CanalEnvio;
import com.skillswap.application.notificacion.NotificacionRepository;
import com.skillswap.domain.notificacion.CanalNotificacion;
import com.skillswap.domain.notificacion.Notificacion;

/*
 * Canal de notificacion interna: "entregar" es guardar en la bandeja del usuario.
 * Es el unico canal que persiste.
 */
public class CanalInterno implements CanalEnvio {

    private final NotificacionRepository repositorio;

    public CanalInterno(NotificacionRepository repositorio) {
        if (repositorio == null) {
            throw new IllegalArgumentException("El repositorio de notificaciones es obligatorio");
        }
        this.repositorio = repositorio;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (notificacion == null) {
            throw new IllegalArgumentException("La notificacion es obligatoria");
        }
        repositorio.guardar(notificacion);
    }

    @Override
    public CanalNotificacion tipo() {
        return CanalNotificacion.INTERNA;
    }
}
