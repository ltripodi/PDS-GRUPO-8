package com.skillswap.infrastructure.adapters.notificacion;

import java.util.ArrayList;
import java.util.List;

/*
 * Proveedor de push simulado (el "Adaptee" del patron Adapter).
 * Tiene su propia firma, distinta de CanalEnvio, y guarda lo emitido para poder verificarlo.
 */
public class PushExternoMock {

    public record PushEmitido(String destinatarioUserId, String contenido) {
    }

    private final List<PushEmitido> emitidos = new ArrayList<>();

    public void emitirPush(String destinatarioUserId, String contenido) {
        if (destinatarioUserId == null || destinatarioUserId.isBlank()) {
            throw new IllegalArgumentException("El destinatario del push es obligatorio");
        }
        emitidos.add(new PushEmitido(destinatarioUserId, contenido));
    }

    public List<PushEmitido> getEmitidos() {
        return List.copyOf(emitidos);
    }
}
