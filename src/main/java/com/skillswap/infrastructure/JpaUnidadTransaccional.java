package com.skillswap.infrastructure;

import com.skillswap.application.UnidadTransaccional;

public class JpaUnidadTransaccional implements UnidadTransaccional {
    @Override
    public void ejecutar(Runnable operacion) {
        JPAUtil.transaccion(em -> {
            operacion.run();
            return null;
        });
    }
}
