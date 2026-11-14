package com.skillswap.application;

import com.skillswap.domain.Notificacion;

import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository {
    void guardar(Notificacion notificacion);

    Optional<Notificacion> buscarPorId(UUID id);

    /* Bandeja paginada, de la mas reciente a la mas vieja. "pagina" es base 0. */
    Pagina<Notificacion> listar(String destinatarioId, int pagina, int tamanio);

    /* Marca como leidas todas las no leidas del destinatario, en una sola operacion. Devuelve cuantas cambio. */
    int marcarTodasLeidas(String destinatarioId);
}