package com.skillswap.application.evento;

import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.ObservadorEvento;

public interface PublicadorEventosPort {
    void publicarDespuesDelCommit(EventoDominio evento);

    void suscribir(ObservadorEvento obs);
}
