package com.skillswap.application.compartido;

public interface UnidadTransaccional {
    void ejecutar(Runnable operacion);
}
