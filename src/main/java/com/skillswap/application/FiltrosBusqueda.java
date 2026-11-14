package com.skillswap.application;

import com.skillswap.domain.Modalidad;
import com.skillswap.domain.OrdenPublicacion;

import java.util.UUID;

/*
 * Criterios de busqueda de publicaciones. Todo filtro es opcional: null significa "no filtrar".
 * Sin orden explicito se ordena por relevancia.
 */
public class FiltrosBusqueda {

    private final String texto;
    private final UUID categoriaId;
    private final Modalidad modalidad;
    private final Integer precioMinimo;
    private final Integer precioMaximo;
    private final UUID habilidadId;
    private final Double reputacionMinima;
    private final OrdenPublicacion orden;

    public FiltrosBusqueda(String texto, UUID categoriaId, Modalidad modalidad, Integer precioMinimo,
                           Integer precioMaximo, UUID habilidadId, Double reputacionMinima,
                           OrdenPublicacion orden) {
        if ((precioMinimo != null && precioMinimo < 0) || (precioMaximo != null && precioMaximo < 0)) {
            throw new IllegalArgumentException("Los precios no pueden ser negativos");
        }
        if (precioMinimo != null && precioMaximo != null && precioMinimo > precioMaximo) {
            throw new IllegalArgumentException("El precio minimo no puede superar al maximo");
        }
        if (reputacionMinima != null && (!Double.isFinite(reputacionMinima) || reputacionMinima < 0)) {
            throw new IllegalArgumentException("La reputacion minima debe ser un numero no negativo");
        }
        this.texto = texto == null || texto.isBlank() ? null : texto.trim();
        this.categoriaId = categoriaId;
        this.modalidad = modalidad;
        this.precioMinimo = precioMinimo;
        this.precioMaximo = precioMaximo;
        this.habilidadId = habilidadId;
        this.reputacionMinima = reputacionMinima;
        this.orden = orden == null ? OrdenPublicacion.RELEVANCIA : orden;
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

    public Integer getPrecioMinimo() {
        return precioMinimo;
    }

    public Integer getPrecioMaximo() {
        return precioMaximo;
    }

    public UUID getHabilidadId() {
        return habilidadId;
    }

    public Double getReputacionMinima() {
        return reputacionMinima;
    }

    public OrdenPublicacion getOrden() {
        return orden;
    }
}