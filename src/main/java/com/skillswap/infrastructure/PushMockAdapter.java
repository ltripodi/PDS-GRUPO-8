package com.skillswap.infrastructure;

import com.skillswap.application.CanalEnvio;
import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Notificacion;

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
