package com.skillswap.application.publicacion;

import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.Modalidad;

import java.util.List;
import java.util.UUID;

/* Publicacion como la ve la interfaz: solo datos, nunca la entidad. Lo arma PublicacionMapper. */
public class PublicacionDTO {
    private final UUID id;
    private final String titulo;
    private final String descripcion;
    private final String categoria;
    private final Modalidad modalidad;
    private final int duracionMinutos;
    private final int precio;
    private final double reputacion;
    private final String proveedor;
    private final List<String> etiquetas;
    private final EstadoPublicacion estado;
    private final List<FranjaDTO> franjas;

    public PublicacionDTO(UUID id, String titulo, String descripcion, String categoria, Modalidad modalidad,
                          int duracionMinutos, int precio, double reputacion, String proveedor,
                          List<String> etiquetas, EstadoPublicacion estado) {
        this(id, titulo, descripcion, categoria, modalidad, duracionMinutos, precio, reputacion, proveedor,
                etiquetas, estado, List.of());
    }

    public PublicacionDTO(UUID id, String titulo, String descripcion, String categoria, Modalidad modalidad,
                          int duracionMinutos, int precio, double reputacion, String proveedor,
                          List<String> etiquetas, EstadoPublicacion estado, List<FranjaDTO> franjas) {
        this.franjas = franjas == null ? List.of() : List.copyOf(franjas);
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.modalidad = modalidad;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
        this.reputacion = reputacion;
        this.proveedor = proveedor;
        this.etiquetas = etiquetas == null ? List.of() : List.copyOf(etiquetas);
        this.estado = estado;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getCategoria() {
        return categoria;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getPrecio() {
        return precio;
    }

    public double getReputacion() {
        return reputacion;
    }

    public String getProveedor() {
        return proveedor;
    }

    public List<String> getEtiquetas() {
        return etiquetas;
    }

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public List<FranjaDTO> getFranjas() {
        return franjas;
    }
}