package com.skillswap.domain;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Publicacion {
    private UUID id;

    /*
     * Autor de una OFERTA: id del Perfil, con clave foranea hacia perfiles.
     * En un DESAFIO queda en null: el autor es la organizacion (organizacion_id, con su propia clave foranea).
     * Exactamente uno de los dos tiene valor; lo garantizan los constructores. Para leer el autor usar getAutorId().
     */
    private UUID autorId;

    private TipoPublicacion tipo;

    private String titulo;

    private String descripcion;

    private Categoria categoria;

    private Modalidad modalidad;

    private SkillCoins precioBase;

    private EstadoPublicacion estado;

    private int cupo;

    private int duracionMinutos;

    private Set<String> etiquetas;

    private LocalDateTime fechaCreacion;

    private List<FranjaHoraria> franjas = new ArrayList<>();

    private Set<Habilidad> habilidades = new HashSet<>();

    /*
     * Navegacion de solo lectura: la columna autor_id se escribe desde autorId.
     * En un desafio autor_id es null, asi que esta relacion tambien: las consultas la usan con LEFT JOIN.
     */
    private Perfil perfil;

    private Organizacion organizacion;

    private List<RegistroTransicion> historial = new ArrayList<>();

    protected Publicacion() {
    }

    /* Oferta de un perfil (mentoria o servicio). Un desafio se crea con crearDesafio. */
    public Publicacion(UUID autorId, TipoPublicacion tipo, String titulo, String descripcion, Categoria categoria,
                       Modalidad modalidad, SkillCoins precioBase, int duracionMinutos, int cupo) {
        this(tipo, titulo, descripcion, categoria, modalidad, precioBase, duracionMinutos, cupo);
        if (autorId == null) {
            throw new IllegalArgumentException("Autor, tipo, categoria, modalidad y precio son obligatorios");
        }
        if (tipo == TipoPublicacion.DESAFIO) {
            throw new IllegalArgumentException("Un desafio se crea con crearDesafio y tiene una organizacion autora");
        }
        this.autorId = autorId;
    }

    /* Datos comunes a ofertas y desafios. El autor lo asigna quien la llama. */
    private Publicacion(TipoPublicacion tipo, String titulo, String descripcion, Categoria categoria,
                        Modalidad modalidad, SkillCoins precioBase, int duracionMinutos, int cupo) {
        if (tipo == null || categoria == null || modalidad == null || precioBase == null) {
            throw new IllegalArgumentException("Autor, tipo, categoria, modalidad y precio son obligatorios");
        }
        if (duracionMinutos <= 0 || cupo <= 0) {
            throw new IllegalArgumentException("La duracion y el cupo deben ser positivos");
        }
        this.id = UUID.randomUUID();
        this.tipo = tipo;
        this.titulo = titulo == null ? "" : titulo.trim();
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.categoria = categoria;
        this.modalidad = modalidad;
        this.precioBase = precioBase;
        this.duracionMinutos = duracionMinutos;
        this.cupo = cupo;
        this.estado = EstadoPublicacion.BORRADOR;
        this.etiquetas = new HashSet<>();
        this.fechaCreacion = LocalDateTime.now();
    }

    public static Publicacion crearOferta(Perfil autor, TipoPublicacion tipo, String titulo, String descripcion,
                                          Categoria categoria, Modalidad modalidad, SkillCoins precioBase,
                                          int duracionMinutos, int cupo) {
        if (autor == null || !autor.isActivo()) {
            throw new IllegalArgumentException("La oferta requiere un perfil activo");
        }
        if (tipo != TipoPublicacion.MENTORIA && tipo != TipoPublicacion.SERVICIO) {
            throw new IllegalArgumentException("Un perfil solo puede crear mentorias o servicios");
        }
        Publicacion publicacion = new Publicacion(autor.getId(), tipo, titulo, descripcion, categoria, modalidad,
                precioBase, duracionMinutos, cupo);
        publicacion.perfil = autor;
        return publicacion;
    }

    public static Publicacion crearDesafio(Organizacion autora, String titulo, String descripcion,
                                           Categoria categoria, Modalidad modalidad, SkillCoins precioBase,
                                           int duracionMinutos, int cupo) {
        if (autora == null) {
            throw new IllegalArgumentException("El desafio requiere una organizacion autora");
        }
        Publicacion publicacion = new Publicacion(TipoPublicacion.DESAFIO, titulo, descripcion, categoria, modalidad,
                precioBase, duracionMinutos, cupo);
        publicacion.organizacion = autora;
        return publicacion;
    }

    public UUID getId() {
        return id;
    }

    /* Id del autor: el del Perfil en una oferta, el de la Organizacion en un desafio. */
    public UUID getAutorId() {
        return organizacion != null ? organizacion.getId() : autorId;
    }

    public TipoPublicacion getTipo() {
        return tipo;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public Modalidad getModalidad() {
        return modalidad;
    }

    public SkillCoins getPrecioBase() {
        return precioBase;
    }

    public EstadoPublicacion getEstado() {
        return estado;
    }

    public int getCupo() {
        return cupo;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public Set<String> getEtiquetas() {
        return etiquetas;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public List<FranjaHoraria> getFranjas() {
        return franjas;
    }

    public Set<Habilidad> getHabilidades() {
        return habilidades;
    }

    /* Solo para ofertas de un perfil. En un desafio es null: usar getOrganizacion(). */
    public Perfil getPerfil() {
        return perfil;
    }

    public Organizacion getOrganizacion() {
        return organizacion;
    }

    public List<RegistroTransicion> getHistorial() {
        return historial;
    }

    public void editarDatos(String titulo, String descripcion, SkillCoins precio) {
        asegurarBorrador();
        if (precio == null) {
            throw new IllegalArgumentException("El precio es obligatorio");
        }
        this.titulo = titulo == null ? "" : titulo.trim();
        this.descripcion = descripcion == null ? "" : descripcion.trim();
        this.precioBase = precio;
    }

    public void publicar(String actorId) {
        if (estado != EstadoPublicacion.BORRADOR && estado != EstadoPublicacion.PAUSADA) {
            throw new IllegalStateException("Solo un borrador o una publicacion pausada puede publicarse");
        }
        validarParaPublicar();
        transicionar(EstadoPublicacion.PUBLICADA, actorId, "publicacion");
    }

    public void pausar(String actorId) {
        if (estado != EstadoPublicacion.PUBLICADA) {
            throw new IllegalStateException("Solo una publicacion publicada puede pausarse");
        }
        transicionar(EstadoPublicacion.PAUSADA, actorId, "pausa");
    }

    public void cerrar(String actorId) {
        if (estado != EstadoPublicacion.PUBLICADA && estado != EstadoPublicacion.PAUSADA) {
            throw new IllegalStateException("Solo una publicacion publicada o pausada puede cerrarse");
        }
        transicionar(EstadoPublicacion.CERRADA, actorId, "cierre");
    }

    public void cancelar(String actorId, String motivo) {
        if (estado == EstadoPublicacion.CERRADA || estado == EstadoPublicacion.CANCELADA) {
            throw new IllegalStateException("Una publicacion cerrada o cancelada no puede cancelarse");
        }
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("La cancelacion requiere un motivo");
        }
        transicionar(EstadoPublicacion.CANCELADA, actorId, motivo.trim());
    }

    public boolean admiteSolicitud() {
        return estado == EstadoPublicacion.PUBLICADA;
    }

    public void agregarFranja(FranjaHoraria franja) {
        asegurarBorrador();
        if (franja == null) {
            throw new IllegalArgumentException("La franja es obligatoria");
        }
        franjas.add(franja);
    }

    public void cambiarClasificacion(Categoria categoria, Modalidad modalidad) {
        asegurarBorrador();
        if (categoria == null || modalidad == null) {
            throw new IllegalArgumentException("La categoria y modalidad son obligatorias");
        }
        this.categoria = categoria;
        this.modalidad = modalidad;
    }

    public void configurarCapacidad(int duracionMinutos, int cupo) {
        asegurarBorrador();
        if (duracionMinutos <= 0 || cupo <= 0) {
            throw new IllegalArgumentException("La duracion y el cupo deben ser positivos");
        }
        this.duracionMinutos = duracionMinutos;
        this.cupo = cupo;
    }

    public void actualizarEtiquetas(Set<String> etiquetas) {
        asegurarBorrador();
        if (etiquetas == null) {
            throw new IllegalArgumentException("Las etiquetas son obligatorias");
        }
        Set<String> normalizadas = new HashSet<>();
        for (String etiqueta : etiquetas) {
            if (etiqueta == null || etiqueta.isBlank()) {
                throw new IllegalArgumentException("Las etiquetas no pueden estar vacias");
            }
            normalizadas.add(etiqueta.trim());
        }
        this.etiquetas.clear();
        this.etiquetas.addAll(normalizadas);
    }

    private void asegurarBorrador() {
        if (estado != EstadoPublicacion.BORRADOR) {
            throw new IllegalStateException("Solo se puede editar una publicacion en borrador");
        }
    }

    private void validarParaPublicar() {
        if (titulo == null || titulo.isBlank()) {
            throw new IllegalStateException("Una publicacion requiere titulo para publicarse");
        }
        if (categoria == null || modalidad == null || precioBase == null || duracionMinutos <= 0 || cupo <= 0) {
            throw new IllegalStateException("La publicacion no tiene todos los datos obligatorios");
        }
        if (franjas.isEmpty() || franjas.stream().anyMatch(franja -> franja == null
                || franja.getInicio() == null || franja.getFin() == null
                || !franja.getInicio().isBefore(franja.getFin()) || franja.getCupo() <= 0)) {
            throw new IllegalStateException("Una publicacion requiere al menos una franja valida");
        }
        if (tipo == TipoPublicacion.DESAFIO && organizacion == null) {
            throw new IllegalStateException("Un desafio requiere una organizacion autora");
        }
        if (tipo != TipoPublicacion.DESAFIO && organizacion != null) {
            throw new IllegalStateException("Solo una organizacion puede ser autora de un desafio");
        }
    }

    private void transicionar(EstadoPublicacion destino, String actorId, String motivo) {
        if (actorId == null || actorId.isBlank()) {
            throw new IllegalArgumentException("El actor de la transicion es obligatorio");
        }
        EstadoPublicacion origen = estado;
        estado = destino;
        historial.add(new RegistroTransicion(actorId.trim(), origen.name(), destino.name(), motivo));
    }
}
