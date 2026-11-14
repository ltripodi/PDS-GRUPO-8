package com.skillswap.domain;

import java.util.UUID;

public class Categoria {
    private UUID id;

    private String nombre;

    protected Categoria() {
    }

    public Categoria(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        this.id = UUID.randomUUID();
        this.nombre = nombre;
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
