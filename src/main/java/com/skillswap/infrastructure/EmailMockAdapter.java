package com.skillswap.infrastructure;

import com.skillswap.application.CanalEnvio;
import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Notificacion;

/*
 * Adapter Pattern - Adapter
 * Traduce CanalEnvio.enviar(Notificacion), que es lo que espera la aplicacion,
 * a EmailExternoMock.enviarCorreo(userId, contenido), que es la firma del proveedor.
 */
public class EmailMockAdapter implements CanalEnvio {

    private final EmailExternoMock cliente;

    public EmailMockAdapter(EmailExternoMock cliente) {
        if (cliente == null) {
            throw new IllegalArgumentException("El proveedor de email es obligatorio");
        }
        this.cliente = cliente;
    }

    @Override
    public void enviar(Notificacion notificacion) {
        if (notificacion == null) {
            throw new IllegalArgumentException("La notificacion es obligatoria");
        }
        cliente.enviarCorreo(notificacion.getDestinatarioId(),
                notificacion.getTitulo() + "\n\n" + notificacion.getDetalle());
    }

    @Override
    public CanalNotificacion tipo() {
        return CanalNotificacion.EMAIL;
    }
}
