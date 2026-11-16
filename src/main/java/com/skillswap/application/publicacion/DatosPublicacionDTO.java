package com.skillswap.application.publicacion;

import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.TipoPublicacion;

import java.util.Set;
import java.util.UUID;

/* Datos de una publicacion que llegan de la interfaz. Solo tipos simples: el precio es un int y el servicio arma SkillCoins. organizacionId es opcional en un desafio: si falta, se usa la organizacion que representa el usuario. */
public class DatosPublicacionDTO {
    private final String titulo;
    private final String descripcion;
    private final UUID categoriaId;
    private final Modalidad modalidad;
    private final int duracionMinutos;
    private final int cupo;
    private final int precioBase;
    private final Set<String> etiquetas;
    private final TipoPublicacion tipo;
    private final UUID organizacionId;

    public DatosPublicacionDTO(String titulo, String descripcion, UUID categoriaId, Modalidad modalidad,
                               int duracionMinutos, int cupo, int precioBase, Set<String> etiquetas,
                               TipoPublicacion tipo, UUID organizacionId) {
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoriaId = categoriaId;
        this.modalidad = modalidad;
        this.duracionMinutos = duracionMinutos;
        this.cupo = cupo;
        this.precioBase = precioBase;
        this.etiquetas = etiquetas == null ? Set.of() : Set.copyOf(etiquetas);
        this.tipo = tipo;
        this.organizacionId = organizacionId;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public UUID getCategoriaId() {
        return categoriaId;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public int getCupo() {
        return cupo;
    }

    public int getPrecioBase() {
        return precioBase;
    }

    public Set<String> getEtiquetas() {
        return etiquetas;
    }

    public TipoPublicacion getTipo() {
        return tipo;
    }

    public UUID getOrganizacionId() {
        return organizacionId;
    }
}
