package com.skillswap.application;

import com.skillswap.domain.Modalidad;
import com.skillswap.domain.OrdenPublicacion;
import jakarta.persistence.Id;

import java.util.UUID;

/** DTO. */
public class FiltrosBusqueda {
    private String texto;
    private UUID categoriaId;
    private Modalidad modalidad;
    private int precioMinimo;
    private int precioMaximo;
    private UUID habilidadId;
    private double reputacionMinima;
    private OrdenPublicacion orden;

    public FiltrosBusqueda(String texto, UUID categoriaId, Modalidad modalidad, int precioMinimo, int precioMaximo,
                           UUID habilidadId, double reputacionMinima, OrdenPublicacion orden) {
        this.texto = texto;
        this.categoriaId = categoriaId;
        this.modalidad = modalidad;
        this.precioMinimo = precioMinimo;
        this.precioMaximo = precioMaximo;
        this.habilidadId = habilidadId;
        this.reputacionMinima = reputacionMinima;
        this.orden = orden;
    }

    public String getTexto() {
        return texto;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public int getPrecioMinimo() {
        return precioMinimo;
    }

    public int getPrecioMaximo() {
        return precioMaximo;
    }

    public UUID getHabilidadId() {
        return habilidadId;
    }

    public double getReputacionMinima() {
        return reputacionMinima;
    }

    public OrdenPublicacion getOrden() {
        return orden;
    }
}
