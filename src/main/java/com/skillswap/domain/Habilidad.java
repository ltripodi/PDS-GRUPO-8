package com.skillswap.domain;

import java.util.Objects;
import java.util.UUID;

public class Habilidad {
    private UUID id;

    private String nombre;

    protected Habilidad() {
    }

    public Habilidad(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }
        this.id = UUID.randomUUID();
        this.nombre = nombre;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Habilidad otra && id != null && id.equals(otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    public UUID getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
}
