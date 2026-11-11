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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
 * State Pattern - Context.
 *
 * La Contratacion NO decide si una accion es valida segun su estado: lo delega en
 * estadoActual (EstadoPendiente, EstadoAceptada, ...). Ella se ocupa de lo que es
 * comun a todas las transiciones: validar quien es el actor, mantener la reserva
 * y registrar cada transicion en el historial inmutable (regla de negocio 5).
 *
 * Los actores se identifican por el id de Perfil (clienteId / proveedorId).
 * El servicio de aplicacion traduce el userId de identidad a ese id.
 */
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
        if (clienteId == null || proveedorId == null || precio == null || solicitud == null) {
            throw new IllegalArgumentException("Cliente, proveedor, precio y solicitud son obligatorios");
        }
        if (clienteId.equals(proveedorId)) {
            throw new IllegalArgumentException("El autor no puede contratar su propia publicacion");
        }

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

    // ------------------------------------------------------------------
    // Acciones de la tabla 4.5: cada una valida el actor, delega en el
    // State y deja asentada la transicion en el historial.
    // ------------------------------------------------------------------

    /*
     * PENDIENTE -> ACEPTADA. La aceptacion crea la reserva y bloquea el cupo de la franja.
     */
    public void aceptar(UUID actorId) {
        validarProveedor(actorId, "aceptar");
        String origen = estado;

        estadoActual.aceptar(this);
        this.reserva = new Reserva(franjaElegida(), 1);

        registrarTransicion(actorId, origen, "Solicitud aceptada por el proveedor");
    }

    /*
     * PENDIENTE -> RECHAZADA. No hay reserva que liberar.
     */
    public void rechazar(UUID actorId) {
        validarProveedor(actorId, "rechazar");
        String origen = estado;

        estadoActual.rechazar(this);

        registrarTransicion(actorId, origen, "Solicitud rechazada por el proveedor");
    }

    /*
     * ACEPTADA -> CONFIRMADA. El movimiento de SkillCoins lo hace PagoService;
     * aca solo se valida y se registra el cambio de estado.
     */
    public void confirmarPago(UUID actorId) {
        validarCliente(actorId, "pagar");
        String origen = estado;

        estadoActual.confirmarPago(this);

        registrarTransicion(actorId, origen, "Pago confirmado con SkillCoins");
    }

    /*
     * ACEPTADA / CONFIRMADA -> CANCELADA. Libera el cupo reservado.
     * El monto del reintegro lo calcula la PoliticaCancelacion (Strategy) en aplicacion.
     */
    public void cancelar(UUID actorId, String motivo) {
        validarParticipante(actorId, "cancelar");
        if (motivo == null || motivo.isBlank()) {
            throw new IllegalArgumentException("La cancelacion requiere un motivo");
        }
        String origen = estado;

        estadoActual.cancelar(this);
        if (reserva != null && reserva.isVigente()) {
            reserva.liberar();
        }

        registrarTransicion(actorId, origen, motivo);
    }

    /*
     * CONFIRMADA -> EN_CURSO. Decision del equipo: inicia el proveedor.
     */
    public void iniciar(UUID actorId) {
        validarProveedor(actorId, "iniciar");
        String origen = estado;

        estadoActual.iniciar(this);

        registrarTransicion(actorId, origen, "Sesion iniciada");
    }

    /*
     * EN_CURSO -> COMPLETADA. Decision del equipo: completa el proveedor.
     */
    public void completar(UUID actorId) {
        validarProveedor(actorId, "completar");
        String origen = estado;

        estadoActual.completar(this);

        registrarTransicion(actorId, origen, "Sesion completada");
    }

    /*
     * Le pregunta al State si habilita calificar (solo COMPLETADA).
     */
    public boolean habilitaCalificacion() {
        return estadoActual.permiteCalificar();
    }

    /*
     * Reprogramacion paso 1: una de las partes propone una nueva franja.
     * Quien propone ya la acepta; falta la aceptacion de la otra parte.
     */
    public void proponerReprogramacion(UUID actorId, FranjaHoraria nuevaFranja) {
        validarParticipante(actorId, "proponer una reprogramacion de");
        estadoActual.proponerReprogramacion(this);
        validarQueNoComenzo();

        if (propuestaPendiente().isPresent()) {
            throw new IllegalStateException("Ya existe una propuesta de reprogramacion pendiente");
        }
        if (nuevaFranja == null || buscarFranjaDePublicacion(nuevaFranja.getId()).isEmpty()) {
            throw new IllegalArgumentException("La nueva franja debe pertenecer a la publicacion");
        }
        if (nuevaFranja.getId().equals(reserva.getFranjaId())) {
            throw new IllegalArgumentException("La nueva franja debe ser distinta a la actual");
        }
        if (!nuevaFranja.hayCupo()) {
            throw new IllegalStateException("La nueva franja no tiene cupo disponible");
        }

        PropuestaReprogramacion propuesta = new PropuestaReprogramacion(id, nuevaFranja.getId());
        aceptarPropuestaSegunRol(propuesta, actorId);
        propuestas.add(propuesta);
    }

    /*
     * Reprogramacion paso 2: la otra parte acepta y, si ya aceptaron ambas,
     * la reserva se mueve a la nueva franja (libera la vieja y ocupa la nueva).
     */
    public void aplicarReprogramacion(UUID actorId) {
        validarParticipante(actorId, "reprogramar");
        estadoActual.aplicarReprogramacion(this);
        validarQueNoComenzo();

        PropuestaReprogramacion propuesta = propuestaPendiente()
                .orElseThrow(() -> new IllegalStateException("No hay una propuesta de reprogramacion pendiente"));

        aceptarPropuestaSegunRol(propuesta, actorId);
        if (!propuesta.aceptadaPorAmbos()) {
            throw new IllegalStateException("La reprogramacion debe aceptarla la otra parte");
        }

        FranjaHoraria nuevaFranja = buscarFranjaDePublicacion(propuesta.getNuevaFranjaId())
                .orElseThrow(() -> new IllegalStateException("La franja propuesta ya no existe"));
        reserva.reprogramar(nuevaFranja);

        registrarTransicion(actorId, estado, "Reprogramada a la franja " + nuevaFranja.getId());
    }

    // ------------------------------------------------------------------
    // Usado solo por los estados concretos (mismo paquete): por eso no es public.
    // Nadie de afuera puede saltear el State y forzar un estado.
    // ------------------------------------------------------------------

    void cambiarEstado(EstadoContratacion nuevoEstado) {
        this.estadoActual = nuevoEstado;
        this.estado = nuevoEstado.nombre();
    }

    // ------------------------------------------------------------------
    // Helpers privados
    // ------------------------------------------------------------------

    public boolean esParticipante(UUID perfilId) {
        return clienteId.equals(perfilId) || proveedorId.equals(perfilId);
    }

    private void validarCliente(UUID actorId, String accion) {
        if (!clienteId.equals(actorId)) {
            throw new SecurityException("Solo el cliente puede " + accion + " la contratacion");
        }
    }

    private void validarProveedor(UUID actorId, String accion) {
        if (!proveedorId.equals(actorId)) {
            throw new SecurityException("Solo el proveedor puede " + accion + " la contratacion");
        }
    }

    private void validarParticipante(UUID actorId, String accion) {
        if (!esParticipante(actorId)) {
            throw new SecurityException("Solo un participante puede " + accion + " la contratacion");
        }
    }

    /*
     * Consigna 4.4: "una reserva puede reprogramarse antes de comenzar".
     */
    private void validarQueNoComenzo() {
        FranjaHoraria actual = buscarFranjaDePublicacion(reserva.getFranjaId())
                .orElseThrow(() -> new IllegalStateException("La franja reservada ya no existe"));
        if (!Instant.now().isBefore(actual.getInicio())) {
            throw new IllegalStateException("La sesion ya comenzo: no puede reprogramarse");
        }
    }

    private void aceptarPropuestaSegunRol(PropuestaReprogramacion propuesta, UUID actorId) {
        if (clienteId.equals(actorId)) {
            propuesta.aceptarComoCliente();
        } else {
            propuesta.aceptarComoProveedor();
        }
    }

    private Optional<PropuestaReprogramacion> propuestaPendiente() {
        return propuestas.stream()
                .filter(propuesta -> !propuesta.aceptadaPorAmbos())
                .findFirst();
    }

    private FranjaHoraria franjaElegida() {
        return buscarFranjaDePublicacion(solicitud.getFranjaId())
                .orElseThrow(() -> new IllegalStateException("La franja elegida no pertenece a la publicacion"));
    }

    private Optional<FranjaHoraria> buscarFranjaDePublicacion(UUID franjaId) {
        return solicitud.getPublicacion().getFranjas().stream()
                .filter(franja -> franja.getId().equals(franjaId))
                .findFirst();
    }

    private void registrarTransicion(UUID actorId, String origen, String motivo) {
        historial.add(new RegistroTransicion(actorId.toString(), origen, estado, motivo));
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

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
}
