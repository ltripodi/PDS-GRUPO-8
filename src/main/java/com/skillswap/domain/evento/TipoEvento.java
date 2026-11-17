package com.skillswap.domain.evento;

/*
 * Los seis primeros son los eventos que la consigna (4.6) obliga a notificar.
 * Los otros tres cubren pasos que sin aviso dejarian a una parte sin enterarse:
 * que le rechazaron la solicitud, que tiene una reprogramacion para aceptar o que se la descartaron.
 */
public enum TipoEvento {
    SOLICITUD_RECIBIDA,
    ACEPTACION,
    PAGO_CONFIRMADO,
    REPROGRAMACION,
    CANCELACION,
    SESION_COMPLETADA,
    RECHAZO,
    REPROGRAMACION_PROPUESTA,
    REPROGRAMACION_RECHAZADA
}
