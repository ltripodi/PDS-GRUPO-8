package com.skillswap.application;

import com.skillswap.domain.Modalidad;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.TipoPublicacion;

import java.util.Set;
import java.util.UUID;

/** DTO. */
public class DatosPublicacionDTO {
    private final String titulo;
    private final String descripcion;
    private final UUID categoriaId;
    private final Modalidad modalidad;
    private final int duracionMinutos;
    private final int cupo;
    private final SkillCoins precioBase;
    private final Set<String> etiquetas;
    private final TipoPublicacion tipo;
    private final UUID organizacionId;

    public DatosPublicacionDTO(String titulo, String descripcion, UUID categoriaId, Modalidad modalidad,
                               int duracionMinutos, int cupo, SkillCoins precioBase, Set<String> etiquetas,
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

    public SkillCoins getPrecioBase() {
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
