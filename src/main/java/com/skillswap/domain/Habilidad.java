package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "habilidades")
public class Habilidad {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
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
