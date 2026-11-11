package com.skillswap.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "publicaciones")
public class Publicacion {
    @Id
    private UUID id;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPublicacion tipo;

    @Column(nullable = false)
    private String titulo;

    @Column(length = 2000)
    private String descripcion;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Modalidad modalidad;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "precio_base"))
    private SkillCoins precioBase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPublicacion estado;

    private int cupo;

    @Column(name = "duracion_minutos")
    private int duracionMinutos;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "publicacion_etiquetas", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Column(name = "etiqueta")
    private Set<String> etiquetas;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "publicacion_id", nullable = false)
    @Fetch(FetchMode.SUBSELECT)
    private List<FranjaHoraria> franjas = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "publicacion_habilidades", joinColumns = @JoinColumn(name = "publicacion_id"), inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private Set<Habilidad> habilidades = new HashSet<>();

    /* Navegacion de solo lectura: la columna autor_id se escribe desde autorId. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id", insertable = false, updatable = false)
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizacion_id")
    private Organizacion organizacion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "publicacion_historial", joinColumns = @JoinColumn(name = "publicacion_id"))
    @Fetch(FetchMode.SUBSELECT)
    private List<RegistroTransicion> historial = new ArrayList<>();

    protected Publicacion() {
    }

    public Publicacion(UUID autorId, TipoPublicacion tipo, String titulo, String descripcion, Categoria categoria,
                       Modalidad modalidad, SkillCoins precioBase, int duracionMinutos, int cupo) {
        if (autorId == null || tipo == null || categoria == null || modalidad == null || precioBase == null) {
            throw new IllegalArgumentException("Autor, tipo, categoria, modalidad y precio son obligatorios");
        }
        if (duracionMinutos <= 0 || cupo <= 0) {
            throw new IllegalArgumentException("La duracion y el cupo deben ser positivos");
        }
        this.id = UUID.randomUUID();
        this.autorId = autorId;
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
        Publicacion publicacion = new Publicacion(autora.getId(), TipoPublicacion.DESAFIO, titulo, descripcion,
                categoria, modalidad, precioBase, duracionMinutos, cupo);
        publicacion.organizacion = autora;
        return publicacion;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAutorId() {
        return autorId;
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
