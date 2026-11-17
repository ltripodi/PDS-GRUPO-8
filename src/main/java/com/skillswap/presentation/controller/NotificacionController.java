package com.skillswap.presentation.controller;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.notificacion.NotificacionDTO;
import com.skillswap.application.notificacion.NotificacionService;
import com.skillswap.domain.notificacion.CanalNotificacion;

import java.util.Set;
import java.util.UUID;

/*
 * Controller de 4.6: valida la entrada y delega en el caso de uso.
 * La bandeja se devuelve paginada; sin pagina explicita, la primera con el tamanio por defecto.
 */
public class NotificacionController extends ControllerBase {

    private final NotificacionService notificacionService;

    public NotificacionController(NotificacionService notificacionService) {
        this.notificacionService = notificacionService;
    }

    public Pagina<NotificacionDTO> listar(String userId) {
        return listar(userId, PRIMERA_PAGINA, TAMANIO_POR_DEFECTO);
    }

    public Pagina<NotificacionDTO> listar(String userId, int pagina, int tamanio) {
        return ejecutar(() -> {
            validarUserId(userId);
            return notificacionService.listar(userId, pagina, tamanio);
        });
    }

    /* El id llega como texto porque asi lo envia la interfaz del kit. */
    public void marcarLeida(String userId, String notificacionId) {
        ejecutarAccion(() -> {
            validarUserId(userId);
            notificacionService.marcarLeida(userId, aUuid(notificacionId));
        });
    }

    public void marcarTodasLeidas(String userId) {
        ejecutarAccion(() -> {
            validarUserId(userId);
            notificacionService.marcarTodasLeidas(userId);
        });
    }

    public void configurarCanales(String userId, Set<CanalNotificacion> canales) {
        ejecutarAccion(() -> {
            validarUserId(userId);
            if (canales == null) {
                throw new IllegalArgumentException("Los canales son obligatorios");
            }
            notificacionService.configurarCanales(userId, canales);
        });
    }

    private static void validarUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
    }

    private static UUID aUuid(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("La notificacion es obligatoria");
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("El id de notificacion no es valido: " + id);
        }
    }
}