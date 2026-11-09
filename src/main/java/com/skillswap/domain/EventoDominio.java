package com.skillswap.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class EventoDominio {
    private TipoEvento tipo;
    private Instant ocurridoEn;
    private UUID sujetoId;
    private String actorUserId;
    private Set<String> destinatarios;
}
