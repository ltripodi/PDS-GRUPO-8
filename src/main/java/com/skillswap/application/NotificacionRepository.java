package com.skillswap.application;

import com.skillswap.domain.Notificacion;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacionRepository {
    void guardar(Notificacion notificacion);

    List<Notificacion> listar(String userId);

    Optional<Notificacion> buscarPorId(UUID id);
}
