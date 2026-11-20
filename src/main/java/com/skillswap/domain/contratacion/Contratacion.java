package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.RegistroTransicion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.FranjaHoraria;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/*
 * State Pattern - Context: delega en estadoActual si una accion es valida.
 * Ella valida el actor (id de Perfil), maneja la reserva y registra el historial.
 */
public class Contratacion {
    private UUID id;

    private UUID clienteId;

    private UUID proveedorId;

    private SkillCoins precio;

    /* State en memoria; en la DB se guarda el nombre. */
    private EstadoContratacion estadoActual;

    private String estado = "PENDIENTE";

    private Solicitud solicitud;

    private Reserva reserva;

    private List<PropuestaReprogramacion> propuestas = new ArrayList<>();

    private List<RegistroTransicion> historial = new ArrayList<>();

    /* Optimistic locking: lo incrementa la persistencia; el dominio no lo modifica. */
    private long version;

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

    private void reconstruirEstadoActual() {
        this.estadoActual = EstadoContratacionBase.desdeNombre(this.estado);
    }

    // Acciones de la tabla 4.5: validar actor -> delegar en el State -> registrar historial.

    /*
     * Crea la reserva y bloquea el cupo. Todo se valida antes de cambiar el estado: si no hay cupo,
     * la contratacion sigue PENDIENTE (nunca queda ACEPTADA sin reserva).
     * Entre solicitar y aceptar pudo pasar tiempo: se revalida la publicacion y la franja.
     */
    public void aceptar(UUID actorId) {
        validarProveedor(actorId, "aceptar");
        if (!solicitud.getPublicacion().admiteSolicitud()) {
            throw new IllegalStateException("La publicacion ya no esta publicada: no se puede aceptar la solicitud");
        }
        FranjaHoraria franja = franjaElegida();
        if (!Instant.now().isBefore(franja.getInicio())) {
            throw new IllegalStateException("La franja elegida ya comenzo o ya paso");
        }
        if (!franja.hayCupo()) {
            throw new IllegalStateException("La franja no tiene cupo disponible");
        }
        String origen = estado;

        estadoActual.aceptar(this);
        this.reserva = new Reserva(franja, 1);

        registrarTransicion(actorId, origen, "Solicitud aceptada por el proveedor");
    }

    public void rechazar(UUID actorId) {
        validarProveedor(actorId, "rechazar");
        String origen = estado;

        estadoActual.rechazar(this);

        registrarTransicion(actorId, origen, "Solicitud rechazada por el proveedor");
    }

    /* El movimiento de SkillCoins lo hace PagoService. */
    public void confirmarPago(UUID actorId) {
        validarCliente(actorId, "pagar");
        if (solicitud.getPublicacion().getEstado() == EstadoPublicacion.CANCELADA) {
            throw new IllegalStateException("La publicacion fue cancelada: no se puede pagar la contratacion");
        }
        String origen = estado;

        estadoActual.confirmarPago(this);

        registrarTransicion(actorId, origen, "Pago confirmado con SkillCoins");
    }

    /* Libera el cupo. El reintegro lo calcula PoliticaCancelacion en el service. */
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

    /* Decision del equipo: inicia el proveedor. */
    public void iniciar(UUID actorId) {
        validarProveedor(actorId, "iniciar");
        String origen = estado;

        estadoActual.iniciar(this);

        registrarTransicion(actorId, origen, "Sesion iniciada");
    }

    /* Decision del equipo: completa el proveedor. */
    public void completar(UUID actorId) {
        validarProveedor(actorId, "completar");
        String origen = estado;

        estadoActual.completar(this);

        registrarTransicion(actorId, origen, "Sesion completada");
    }

    public boolean habilitaCalificacion() {
        return estadoActual.permiteCalificar();
    }

    /* La regla pertenece a la contratacion: conoce su estado y las dos partes involucradas. */
    public void validarCalificacion(UUID calificadorId, UUID calificadoId) {
        if (calificadorId == null || calificadoId == null) {
            throw new IllegalArgumentException("Calificador y calificado son obligatorios");
        }
        if (!"COMPLETADA".equals(getEstado())) {
            throw new IllegalStateException("Solo una contratacion completada puede calificarse");
        }
        if (calificadorId.equals(calificadoId)) {
            throw new IllegalArgumentException("Una persona no puede calificarse a si misma");
        }
        validarParticipante(calificadorId, "calificar la contratacion");

        UUID contraparte = clienteId.equals(calificadorId) ? proveedorId : clienteId;
        if (!contraparte.equals(calificadoId)) {
            throw new IllegalArgumentException("La calificacion debe dirigirse a la otra parte de la contratacion");
        }
    }

    /* Reprogramacion: quien propone ya acepta*/
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

    /* Reprogramacion : la otra parte acepta y la reserva se mueve de franja. */
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

    /*
     * Reprogramacion: si una parte no acepta, la propuesta se descarta y la reserva queda igual.
     * Sirve tanto para que la otra parte la rechace como para que quien la propuso la retire.
     */
    public void rechazarReprogramacion(UUID actorId) {
        validarParticipante(actorId, "rechazar una reprogramacion de");
        // Solo en los estados que admiten reprogramar (ACEPTADA, CONFIRMADA): nunca en CANCELADA o COMPLETADA.
        estadoActual.proponerReprogramacion(this);

        PropuestaReprogramacion propuesta = propuestaPendiente()
                .orElseThrow(() -> new IllegalStateException("No hay una propuesta de reprogramacion pendiente"));
        propuesta.rechazar();

        registrarTransicion(actorId, estado, "Propuesta de reprogramacion rechazada");
    }

    /* Package-private: solo los estados pueden cambiar el estado. */
    void cambiarEstado(EstadoContratacion nuevoEstado) {
        this.estadoActual = nuevoEstado;
        this.estado = nuevoEstado.nombre();
    }

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

    /* Consigna 4.4: solo se reprograma antes de comenzar. */
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
                .filter(PropuestaReprogramacion::estaPendiente)
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

    public long getVersion() {
        return version;
    }

    public List<PropuestaReprogramacion> getPropuestas() {
        return List.copyOf(propuestas);
    }

    /* Historial inmutable (seccion 5): solo registrarTransicion agrega entradas. */
    public List<RegistroTransicion> getHistorial() {
        return Collections.unmodifiableList(historial);
    }
}
