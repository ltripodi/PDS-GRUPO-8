package com.skillswap.application;

import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Notificacion;

public interface CanalEnvio {
    void enviar(Notificacion notificacion);

    CanalNotificacion tipo();
}
