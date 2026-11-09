package com.skillswap.application;

import com.skillswap.domain.Pago;

import java.util.Optional;
import java.util.UUID;

public interface PagoRepository {
    Optional<Pago> buscarPorContratacion(UUID contratacionId);

    void guardar(Pago pago);
}
