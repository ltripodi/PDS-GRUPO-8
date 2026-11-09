package com.skillswap.application;

import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Modalidad;

import java.util.List;
import java.util.UUID;

/** DTO. */
public class PublicacionDTO {
    private UUID id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private Modalidad modalidad;
    private int duracionMinutos;
    private int precio;
    private double reputacion;
    private String proveedor;
    private List<String> etiquetas;
    private EstadoPublicacion estado;
    private boolean destacada;
}
