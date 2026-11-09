package com.skillswap.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "perfiles")
public class Perfil {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private String userId;

    @Column(name = "nombre_visible", nullable = false)
    private String nombreVisible;

    @Column(length = 1000)
    private String biografia;

    @Column(name = "zona_horaria")
    private ZoneId zonaHoraria;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "perfil_habilidades", joinColumns = @JoinColumn(name = "perfil_id"), inverseJoinColumns = @JoinColumn(name = "habilidad_id"))
    private Set<Habilidad> habilidades;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_intereses", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "interes")
    private Set<String> intereses;

    private boolean activo;

    private double reputacion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_disponibilidad", joinColumns = @JoinColumn(name = "perfil_id"))
    @Fetch(FetchMode.SUBSELECT)
    private List<DisponibilidadPerfil> disponibilidad = new ArrayList<>();

    @Embedded
    private PreferenciaNotificacion preferenciaNotificacion = new PreferenciaNotificacion(EnumSet.of(CanalNotificacion.INTERNA));

    protected Perfil() {
    }

    public Perfil(String userId, String nombreVisible, String biografia, ZoneId zonaHoraria) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.nombreVisible = nombreVisible;
        this.biografia = biografia;
        this.zonaHoraria = zonaHoraria;
        this.habilidades = new HashSet<>();
        this.intereses = new HashSet<>();
        this.activo = true;
        this.reputacion = 0;
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getNombreVisible() {
        return nombreVisible;
    }

    public String getBiografia() {
        return biografia;
    }

    public ZoneId getZonaHoraria() {
        return zonaHoraria;
    }

    public Set<Habilidad> getHabilidades() {
        return habilidades;
    }

    public Set<String> getIntereses() {
        return intereses;
    }

    public boolean isActivo() {
        return activo;
    }

    public double getReputacion() {
        return reputacion;
    }

    public List<DisponibilidadPerfil> getDisponibilidad() {
        return disponibilidad;
    }

    public PreferenciaNotificacion getPreferenciaNotificacion() {
        return preferenciaNotificacion;
    }

    public void actualizarDatos(String nombre, String biografia, ZoneId zona) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void agregarHabilidad(Habilidad habilidad) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void quitarHabilidad(Habilidad habilidad) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void definirDisponibilidad(List<DisponibilidadPerfil> intervalos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void darDeBaja() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void recalcularReputacion(List<Calificacion> calificacionesVerificadas) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void actualizarIntereses(Set<String> intereses) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public double obtenerReputacion() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
