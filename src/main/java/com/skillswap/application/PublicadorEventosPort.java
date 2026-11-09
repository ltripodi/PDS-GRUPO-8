package com.skillswap.application;

import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.ObservadorEvento;

public interface PublicadorEventosPort {
    void publicarDespuesDelCommit(EventoDominio evento);

    void suscribir(ObservadorEvento obs);
}
