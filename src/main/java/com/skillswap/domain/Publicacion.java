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
        this.id = UUID.randomUUID();
        this.autorId = autorId;
        this.tipo = tipo;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.categoria = categoria;
        this.modalidad = modalidad;
        this.precioBase = precioBase;
        this.duracionMinutos = duracionMinutos;
        this.cupo = cupo;
        this.estado = EstadoPublicacion.BORRADOR;
        this.etiquetas = new HashSet<>();
        this.fechaCreacion = LocalDateTime.now();
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
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void publicar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void pausar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cerrar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cancelar(String actorId, String motivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean admiteSolicitud() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void agregarFranja(FranjaHoraria franja) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cambiarClasificacion(Categoria categoria, Modalidad modalidad) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void configurarCapacidad(int duracionMinutos, int cupo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void actualizarEtiquetas(Set<String> etiquetas) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
