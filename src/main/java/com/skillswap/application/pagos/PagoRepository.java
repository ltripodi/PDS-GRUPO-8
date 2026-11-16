package com.skillswap.application.pagos;

import com.skillswap.domain.pagos.Pago;

import java.util.Optional;
import java.util.UUID;

public interface PagoRepository {
    Optional<Pago> buscarPorContratacion(UUID contratacionId);

    void guardar(Pago pago);
}
