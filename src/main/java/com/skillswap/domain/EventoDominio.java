package com.skillswap.domain;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/*
 * Evento de dominio inmutable (Observer: es el "mensaje" que reciben los observadores).
 */
public class EventoDominio {
    private final TipoEvento tipo;
    private final Instant ocurridoEn;
    private final UUID sujetoId;
    private final String actorUserId;
    private final Set<String> destinatarios;

    public EventoDominio(TipoEvento tipo, UUID sujetoId, String actorUserId, Set<String> destinatarios) {
        if (tipo == null || sujetoId == null) {
            throw new IllegalArgumentException("El tipo y el sujeto del evento son obligatorios");
        }
        this.tipo = tipo;
        this.ocurridoEn = Instant.now();
        this.sujetoId = sujetoId;
        this.actorUserId = actorUserId;
        this.destinatarios = destinatarios == null ? Set.of() : Set.copyOf(destinatarios);
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }

    public UUID getSujetoId() {
        return sujetoId;
    }

    public String getActorUserId() {
        return actorUserId;
    }

    public Set<String> getDestinatarios() {
        return destinatarios;
    }
}
