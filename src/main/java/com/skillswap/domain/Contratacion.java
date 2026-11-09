package com.skillswap.domain;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "contrataciones")
public class Contratacion {
    @Id
    private UUID id;

    @Column(name = "cliente_id", nullable = false)
    private UUID clienteId;

    @Column(name = "proveedor_id", nullable = false)
    private UUID proveedorId;

    @Embedded
    @AttributeOverride(name = "valor", column = @Column(name = "precio"))
    private SkillCoins precio;

    /*
     * State: el objeto es comportamiento en memoria; en la DB se guarda el nombre (campo "estado").
     */
    @Transient
    private EstadoContratacion estadoActual;

    @Column(nullable = false)
    private String estado = "PENDIENTE";

    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE}, fetch = FetchType.EAGER)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "reserva_id")
    private Reserva reserva;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "contratacion_id", insertable = false, updatable = false)
    @Fetch(FetchMode.SUBSELECT)
    private List<PropuestaReprogramacion> propuestas = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "contratacion_historial", joinColumns = @JoinColumn(name = "contratacion_id"))
    @Fetch(FetchMode.SUBSELECT)
    private List<RegistroTransicion> historial = new ArrayList<>();

    protected Contratacion() {
    }

    public Contratacion(UUID clienteId, UUID proveedorId, SkillCoins precio, Solicitud solicitud) {
        this.id = UUID.randomUUID();
        this.clienteId = clienteId;
        this.proveedorId = proveedorId;
        this.precio = precio;
        this.solicitud = solicitud;
        this.estadoActual = new EstadoPendiente();
        this.estado = estadoActual.nombre();
    }

    /*
     * Reconstruye el objeto State a partir del nombre persistido en la DB.
     */
    @PostLoad
    private void reconstruirEstadoActual() {
        this.estadoActual = EstadoContratacionBase.desdeNombre(this.estado);
    }

    public UUID getId() {
        return id;
    }

    public UUID getClienteId() {
        return clienteId;
    }

    public UUID getProveedorId() {
        return proveedorId;
    }

    public SkillCoins getPrecio() {
        return precio;
    }

    public EstadoContratacion getEstadoActual() {
        return estadoActual;
    }

    public String getEstado() {
        return estado;
    }

    public Solicitud getSolicitud() {
        return solicitud;
    }

    public Reserva getReserva() {
        return reserva;
    }

    public List<PropuestaReprogramacion> getPropuestas() {
        return propuestas;
    }

    public List<RegistroTransicion> getHistorial() {
        return historial;
    }

    public void aceptar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void rechazar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void confirmarPago(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cancelar(String actorId, String motivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void iniciar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void completar(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public boolean habilitaCalificacion() {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void proponerReprogramacion(String actorId, FranjaHoraria nuevaFranja) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void aplicarReprogramacion(String actorId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
