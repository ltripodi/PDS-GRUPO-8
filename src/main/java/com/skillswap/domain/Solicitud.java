package com.skillswap.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/* Solicitud de un cliente (consigna 4.4). Las reglas de creacion estan en crear(...). */
@Entity
@Table(name = "solicitudes")
public class Solicitud {
    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(length = 1000)
    private String mensaje;

    private String objetivo;

    @Column(name = "franja_id")
    private UUID franjaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicacion_id", nullable = false)
    private Publicacion publicacion;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;

    protected Solicitud() {
    }

    public Solicitud(UUID clienteId, Publicacion publicacion, UUID franjaId, String mensaje, String objetivo) {
        if (clienteId == null || publicacion == null) {
            throw new IllegalArgumentException("El cliente y la publicacion son obligatorios");
        }
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.publicacion = publicacion;
        this.franjaId = franjaId;
        this.mensaje = mensaje;
        this.objetivo = objetivo;
        this.fechaCreacion = Instant.now();
    }

    /* Factory Method con las reglas de negocio de la seccion 5. */
    public static Solicitud crear(Perfil cliente, Publicacion publicacion, UUID franjaId,
                                  String mensaje, String objetivo) {
        if (cliente == null || !cliente.isActivo()) {
            throw new IllegalArgumentException("La solicitud requiere un perfil activo");
        }
        if (publicacion == null) {
            throw new IllegalArgumentException("La publicacion es obligatoria");
        }
        if (cliente.getId().equals(publicacion.getAutorId())) {
            throw new IllegalArgumentException("El autor no puede solicitar su propia publicacion");
        }
        if (!publicacion.admiteSolicitud()) {
            throw new IllegalStateException("Una publicacion no publicada no admite solicitudes");
        }
        if (objetivo == null || objetivo.isBlank()) {
            throw new IllegalArgumentException("La solicitud requiere un objetivo");
        }

        FranjaHoraria franja = publicacion.getFranjas().stream()
                .filter(f -> f.getId().equals(franjaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("La franja elegida no pertenece a la publicacion"));

        // Chequeo temprano: el cupo se bloquea recien al aceptar.
        if (!franja.hayCupo()) {
            throw new IllegalStateException("La franja elegida no tiene cupo disponible");
        }

        String mensajeLimpio = mensaje == null ? "" : mensaje.trim();
        return new Solicitud(cliente.getId(), publicacion, franjaId, mensajeLimpio, objetivo.trim());
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public String getMensaje() {
        return mensaje;
    }

    public String getObjetivo() {
        return objetivo;
    }

    public UUID getFranjaId() {
        return franjaId;
    }

    public Publicacion getPublicacion() {
        return publicacion;
    }

    public Instant getFechaCreacion() {
        return fechaCreacion;
    }
}
