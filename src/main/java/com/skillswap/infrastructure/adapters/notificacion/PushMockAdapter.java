package com.skillswap.infrastructure.adapters.notificacion;

import com.skillswap.application.notificacion.CanalEnvio;
import com.skillswap.domain.notificacion.CanalNotificacion;
import com.skillswap.domain.notificacion.Notificacion;

/*
 * Adapter Pattern - Adapter
 * Traduce CanalEnvio.enviar(Notificacion) a PushExternoMock.emitirPush(userId, contenido).
 */
public class PushMockAdapter implements CanalEnvio {

    private final PushExternoMock cliente;

    public PushMockAdapter(PushExternoMock cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El proveedor de push es obligatorio");
        }
        this.cliente = cliente;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (notificacion == null) {
            throw new IllegalArgumentException("La notificacion es obligatoria");
        }
        cliente.emitirPush(notificacion.getDestinatarioId(), notificacion.getTitulo());
    }

    @Override
    public CanalNotificacion tipo() {
        return CanalNotificacion.PUSH;
    }
}
