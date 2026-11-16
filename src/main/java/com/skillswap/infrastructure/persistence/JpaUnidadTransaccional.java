package com.skillswap.infrastructure.persistence;

import com.skillswap.application.compartido.UnidadTransaccional;

public class JpaUnidadTransaccional implements UnidadTransaccional {
    @Override
    public void ejecutar(Runnable operacion) {
        JPAUtil.transaccion(em -> {
            operacion.run();
            return null;
        });
    }
}
