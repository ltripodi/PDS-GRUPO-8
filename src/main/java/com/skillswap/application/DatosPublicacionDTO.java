package com.skillswap.application;

import com.skillswap.domain.Modalidad;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.TipoPublicacion;

import java.util.Set;
import java.util.UUID;

/** DTO. */
public class DatosPublicacionDTO {
    private String titulo;
    private String descripcion;
    private UUID categoriaId;
    private Modalidad modalidad;
    private int duracionMinutos;
    private int cupo;
    private SkillCoins precioBase;
    private Set<String> etiquetas;
    private TipoPublicacion tipo;
    private UUID organizacionId;
}
