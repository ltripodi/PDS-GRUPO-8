package com.skillswap.application.notificacion;

import com.skillswap.domain.notificacion.CanalNotificacion;
import com.skillswap.domain.notificacion.Notificacion;

public interface CanalEnvio {
    void enviar(Notificacion notificacion);

    CanalNotificacion tipo();
}
