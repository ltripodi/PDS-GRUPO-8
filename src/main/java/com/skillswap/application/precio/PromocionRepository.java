package com.skillswap.application.precio;

import com.skillswap.domain.precio.Promocion;

import java.util.List;

/** Puerto de las promociones vigentes de la plataforma, que se acumulan sobre el precio base (decorator). */
public interface PromocionRepository {
    List<Promocion> listarVigentes();

    void guardar(Promocion promocion);
}
