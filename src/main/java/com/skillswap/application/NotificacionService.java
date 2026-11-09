package com.skillswap.application;

import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.Notificacion;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public class NotificacionService {
    private NotificacionRepository notificaciones;
    private PerfilRepository perfiles;
    private List<CanalEnvio> canales;

    public List<Notificacion> listar(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void marcarLeida(String userId, UUID id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void marcarTodasLeidas(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void configurarCanales(String userId, Set<CanalNotificacion> canales) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void notificar(EventoDominio evento) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Notificacion> listarPaginado(String userId, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
