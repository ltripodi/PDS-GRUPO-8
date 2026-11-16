package com.skillswap.application.contratacion;

import com.skillswap.domain.contratacion.Solicitud;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface SolicitudRepository {
    void guardar(Solicitud solicitud);

    Map<UUID, Integer> contarRecientesPorPublicacion(List<UUID> publicacionIds, Instant desde);
}
