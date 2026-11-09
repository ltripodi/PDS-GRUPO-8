package com.skillswap.infrastructure;

import com.skillswap.application.CanalEnvio;
import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Notificacion;

public class PushMockAdapter implements CanalEnvio {
    private PushExternoMock cliente;

    @Override
    public void enviar(Notificacion notificacion) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    @Override
    public CanalNotificacion tipo() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
