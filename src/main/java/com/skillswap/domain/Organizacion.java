package com.skillswap.domain;

import java.util.UUID;

public class Organizacion {
    private UUID id;

    private String nombre;

    private String userIdAutorizado;

    protected Organizacion() {
    }

    public Organizacion(String nombre, String userIdAutorizado) {
        this.id = UUID.randomUUID();
        this.nombre = nombre;
        this.userIdAutorizado = userIdAutorizado;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getUserIdAutorizado() {
        return userIdAutorizado;
    }

    public boolean autoriza(String userId) {
        return userIdAutorizado != null && userIdAutorizado.equals(userId);
    }
}
