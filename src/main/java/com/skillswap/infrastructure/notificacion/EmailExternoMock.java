package com.skillswap.infrastructure.notificacion;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/*
 * Proveedor de email simulado (el "Adaptee" del patron Adapter).
 * Tiene su propia firma, distinta de CanalEnvio, y guarda (de forma segura entre hilos: los avisos salen de distintas transacciones) lo enviado para poder verificarlo.
 */
public class EmailExternoMock {

    public record CorreoEnviado(String destinatarioUserId, String contenido) {
    }

    private final List<CorreoEnviado> enviados = new CopyOnWriteArrayList<>();

    public void enviarCorreo(String destinatarioUserId, String contenido) {
        if (destinatarioUserId == null || destinatarioUserId.isBlank()) {
            throw new IllegalArgumentException("El destinatario del correo es obligatorio");
        }
        enviados.add(new CorreoEnviado(destinatarioUserId, contenido));
    }

    public List<CorreoEnviado> getEnviados() {
        return List.copyOf(enviados);
    }
}
