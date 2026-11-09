package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "organizaciones")
public class Organizacion {
    @Id
    private UUID id;

    @Column(nullable = false)
    private String nombre;

    @Column(name = "user_id_autorizado", nullable = false)
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
