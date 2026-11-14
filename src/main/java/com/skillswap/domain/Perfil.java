package com.skillswap.domain;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Perfil {
    private UUID id;

    private String userId;

    private String nombreVisible;

    private String biografia;

    private ZoneId zonaHoraria;

    private Set<Habilidad> habilidades;

    private Set<String> intereses;

    private boolean activo;

    private double reputacion;

    private List<DisponibilidadPerfil> disponibilidad = new ArrayList<>();

    private PreferenciaNotificacion preferenciaNotificacion = new PreferenciaNotificacion(EnumSet.of(CanalNotificacion.INTERNA));

    protected Perfil() {
    }

    public Perfil(String userId, String nombreVisible, String biografia, ZoneId zonaHoraria) {
        this.id = UUID.randomUUID();
        this.userId = validarUserId(userId);
        this.nombreVisible = validarNombre(nombreVisible);
        this.biografia = normalizarBiografia(biografia);
        this.zonaHoraria = validarZona(zonaHoraria);
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
        asegurarActivo();
        this.nombreVisible = validarNombre(nombre);
        this.biografia = normalizarBiografia(biografia);
        this.zonaHoraria = validarZona(zona);
    }

    public void agregarHabilidad(Habilidad habilidad) {
        asegurarActivo();
        if (habilidad == null) {
            throw new IllegalArgumentException("La habilidad es obligatoria");
        }
        habilidades.add(habilidad);
    }

    public void quitarHabilidad(Habilidad habilidad) {
        asegurarActivo();
        if (habilidad == null) {
            throw new IllegalArgumentException("La habilidad es obligatoria");
        }
        habilidades.remove(habilidad);
    }

    public void definirDisponibilidad(List<DisponibilidadPerfil> intervalos) {
        asegurarActivo();
        if (intervalos == null || intervalos.stream().anyMatch(intervalo -> intervalo == null || !intervalo.esValida())) {
            throw new IllegalArgumentException("La disponibilidad contiene intervalos invalidos");
        }
        disponibilidad.clear();
        disponibilidad.addAll(intervalos);
    }

    public void darDeBaja() {
        activo = false;
    }

    public void recalcularReputacion(List<Calificacion> calificacionesVerificadas) {
        if (calificacionesVerificadas == null) {
            throw new IllegalArgumentException("Las calificaciones son obligatorias");
        }
        reputacion = calificacionesVerificadas.stream()
                .mapToInt(Calificacion::getPuntaje)
                .average()
                .orElse(0.0);
    }

    public void actualizarIntereses(Set<String> intereses) {
        asegurarActivo();
        if (intereses == null) {
            throw new IllegalArgumentException("Los intereses son obligatorios");
        }
        Set<String> normalizados = new HashSet<>();
        for (String interes : intereses) {
            if (interes == null || interes.isBlank()) {
                throw new IllegalArgumentException("Los intereses no pueden estar vacios");
            }
            normalizados.add(interes.trim());
        }
        this.intereses.clear();
        this.intereses.addAll(normalizados);
    }

    public double obtenerReputacion() {
        return reputacion;
    }

    public void reemplazarHabilidades(Collection<Habilidad> nuevasHabilidades) {
        asegurarActivo();
        if (nuevasHabilidades == null || nuevasHabilidades.stream().anyMatch(habilidad -> habilidad == null)) {
            throw new IllegalArgumentException("Las habilidades no pueden contener valores nulos");
        }
        habilidades.clear();
        habilidades.addAll(nuevasHabilidades);
    }

    private void asegurarActivo() {
        if (!activo) {
            throw new IllegalStateException("No se puede modificar un perfil dado de baja");
        }
    }

    private static String validarUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        return userId.trim();
    }

    private static String validarNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("El nombre visible es obligatorio");
        }
        return nombre.trim();
    }

    private static ZoneId validarZona(ZoneId zona) {
        if (zona == null) {
            throw new IllegalArgumentException("La zona horaria es obligatoria");
        }
        return zona;
    }

    private static String normalizarBiografia(String biografia) {
        return biografia == null ? "" : biografia.trim();
    }
}
