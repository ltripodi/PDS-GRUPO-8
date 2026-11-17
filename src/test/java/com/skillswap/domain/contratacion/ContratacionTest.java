package com.skillswap.domain.contratacion;

import com.skillswap.domain.compartido.RegistroTransicion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/* Contratacion (4.4 y 4.5): State, reserva, cancelacion, reprogramacion y solicitud, sin servicios. */
class ContratacionTest {

    private Perfil cliente;
    private Perfil proveedor;
    private Publicacion publicacion;
    private FranjaHoraria franja;
    private FranjaHoraria otraFranja;

    @BeforeEach
    void prepararPublicacionPublicada() {
        cliente = new Perfil("cliente", "Cliente", "", ZoneId.of("UTC"));
        proveedor = new Perfil("proveedor", "Proveedor", "", ZoneId.of("UTC"));

        publicacion = Publicacion.crearOferta(proveedor, TipoPublicacion.MENTORIA, "Mentoria Java", "Desc",
                new Categoria("Tecnologia"), Modalidad.REMOTA, new SkillCoins(100), 60, 1);
        franja = new FranjaHoraria(Instant.parse("2030-01-01T10:00:00Z"), Instant.parse("2030-01-01T11:00:00Z"), 1);
        otraFranja = new FranjaHoraria(Instant.parse("2030-01-02T10:00:00Z"), Instant.parse("2030-01-02T11:00:00Z"), 1);
        publicacion.agregarFranja(franja);
        publicacion.agregarFranja(otraFranja);
        publicacion.publicar("proveedor");
    }

    private Contratacion nuevaContratacion(FranjaHoraria elegida) {
        Solicitud solicitud = Solicitud.crear(cliente, publicacion, elegida.getId(), "Hola", "Preparar entrevista");
        return new Contratacion(cliente.getId(), proveedor.getId(), new SkillCoins(100), solicitud);
    }

    // ---------- State ----------

    @Test
    void flujoCompletoRecorreLosEstadosYRegistraElHistorial() {
        Contratacion contratacion = nuevaContratacion(franja);
        assertEquals("PENDIENTE", contratacion.getEstado());

        contratacion.aceptar(proveedor.getId());
        assertEquals("ACEPTADA", contratacion.getEstado());
        assertEquals(1, franja.getOcupados()); // la aceptacion bloquea el cupo

        contratacion.confirmarPago(cliente.getId());
        contratacion.iniciar(proveedor.getId());
        assertFalse(contratacion.habilitaCalificacion());
        contratacion.completar(proveedor.getId());

        assertEquals("COMPLETADA", contratacion.getEstado());
        assertTrue(contratacion.habilitaCalificacion());
        assertEquals(4, contratacion.getHistorial().size());
        assertEquals("PENDIENTE", contratacion.getHistorial().get(0).getOrigen());
        assertEquals("ACEPTADA", contratacion.getHistorial().get(0).getDestino());
        assertEquals(proveedor.getId().toString(), contratacion.getHistorial().get(0).getActorId());
    }

    @Test
    void transicionInvalidaEsRechazadaPorElDominioSinCambiarElEstado() {
        Contratacion contratacion = nuevaContratacion(franja);

        assertThrows(IllegalStateException.class, () -> contratacion.confirmarPago(cliente.getId()));
        assertThrows(IllegalStateException.class, () -> contratacion.completar(proveedor.getId()));
        assertEquals("PENDIENTE", contratacion.getEstado());

        contratacion.rechazar(proveedor.getId());
        assertThrows(IllegalStateException.class, () -> contratacion.aceptar(proveedor.getId()));
        assertThrows(IllegalStateException.class, () -> contratacion.cancelar(cliente.getId(), "motivo"));
        assertEquals("RECHAZADA", contratacion.getEstado());
    }

    @Test
    void cadaAccionLaPuedeHacerSoloLaParteQueCorresponde() {
        Contratacion contratacion = nuevaContratacion(franja);
        UUID ajeno = UUID.randomUUID();

        assertThrows(SecurityException.class, () -> contratacion.aceptar(cliente.getId()));
        assertThrows(SecurityException.class, () -> contratacion.aceptar(ajeno));

        contratacion.aceptar(proveedor.getId());
        assertThrows(SecurityException.class, () -> contratacion.confirmarPago(proveedor.getId()));
        assertThrows(SecurityException.class, () -> contratacion.cancelar(ajeno, "motivo"));
    }

    @Test
    void elEstadoSeReconstruyeDesdeElNombrePersistido() {
        assertEquals("EN_CURSO", EstadoContratacionBase.desdeNombre("EN_CURSO").nombre());
        assertEquals("PENDIENTE", EstadoContratacionBase.desdeNombre(null).nombre());
        assertThrows(IllegalArgumentException.class, () -> EstadoContratacionBase.desdeNombre("INVENTADO"));
    }

    // ---------- reserva y cancelacion ----------

    @Test
    void noSePuedeAceptarSiLaFranjaNoTieneCupo() {
        Contratacion primera = nuevaContratacion(franja);
        Contratacion segunda = nuevaContratacion(franja);

        primera.aceptar(proveedor.getId());

        assertThrows(IllegalStateException.class, () -> segunda.aceptar(proveedor.getId()));
        assertEquals(1, franja.getOcupados());
    }

    @Test
    void cancelarExigeMotivoYLiberaElCupo() {
        Contratacion contratacion = nuevaContratacion(franja);
        contratacion.aceptar(proveedor.getId());

        assertThrows(IllegalArgumentException.class, () -> contratacion.cancelar(cliente.getId(), " "));

        contratacion.cancelar(cliente.getId(), "No puedo asistir");

        assertEquals("CANCELADA", contratacion.getEstado());
        assertEquals(0, franja.getOcupados());
        assertFalse(contratacion.getReserva().isVigente());
        assertEquals("No puedo asistir", contratacion.getHistorial().get(1).getMotivo());
    }

    // ---------- reprogramacion ----------

    @Test
    void laReprogramacionSoloSeAplicaCuandoAceptanAmbasPartes() {
        Contratacion contratacion = nuevaContratacion(franja);
        contratacion.aceptar(proveedor.getId());
        contratacion.confirmarPago(cliente.getId());

        contratacion.proponerReprogramacion(cliente.getId(), otraFranja);

        // quien propone no puede aceptar por la otra parte
        assertThrows(IllegalStateException.class, () -> contratacion.aplicarReprogramacion(cliente.getId()));
        assertEquals(franja.getId(), contratacion.getReserva().getFranjaId());

        contratacion.aplicarReprogramacion(proveedor.getId());

        assertEquals(otraFranja.getId(), contratacion.getReserva().getFranjaId());
        assertEquals(0, franja.getOcupados());
        assertEquals(1, otraFranja.getOcupados());
        assertEquals("CONFIRMADA", contratacion.getEstado());
    }

    @Test
    void unaPropuestaRechazadaNoMueveLaReservaYPermiteProponerOtra() {
        Contratacion contratacion = nuevaContratacion(franja);
        contratacion.aceptar(proveedor.getId());
        contratacion.proponerReprogramacion(cliente.getId(), otraFranja);

        contratacion.rechazarReprogramacion(proveedor.getId());

        assertEquals(franja.getId(), contratacion.getReserva().getFranjaId());
        assertEquals(0, otraFranja.getOcupados());
        assertTrue(contratacion.getPropuestas().get(0).isRechazada());
        assertThrows(IllegalStateException.class, () -> contratacion.aplicarReprogramacion(proveedor.getId()));
        assertThrows(IllegalStateException.class, () -> contratacion.rechazarReprogramacion(proveedor.getId()));
        assertThrows(SecurityException.class, () -> contratacion.rechazarReprogramacion(UUID.randomUUID()));

        // ya no hay propuesta pendiente: se puede proponer de nuevo
        contratacion.proponerReprogramacion(proveedor.getId(), otraFranja);
        contratacion.aplicarReprogramacion(cliente.getId());
        assertEquals(otraFranja.getId(), contratacion.getReserva().getFranjaId());
    }

    @Test
    void elHistorialNoSePuedeModificarDesdeAfuera() {
        Contratacion contratacion = nuevaContratacion(franja);
        contratacion.rechazar(proveedor.getId());

        assertThrows(UnsupportedOperationException.class, () -> contratacion.getHistorial().clear());
        assertThrows(UnsupportedOperationException.class,
                () -> contratacion.getHistorial().add(new RegistroTransicion("x", "RECHAZADA", "ACEPTADA", "trampa")));
        assertEquals(1, contratacion.getHistorial().size());
    }

    @Test
    void noSePuedeReprogramarUnaSesionEnCurso() {
        Contratacion contratacion = nuevaContratacion(franja);
        contratacion.aceptar(proveedor.getId());
        contratacion.confirmarPago(cliente.getId());
        contratacion.iniciar(proveedor.getId());

        assertThrows(IllegalStateException.class,
                () -> contratacion.proponerReprogramacion(cliente.getId(), otraFranja));
    }

    // ---------- solicitud ----------

    @Test
    void solicitudRespetaLasReglasDeNegocio() {
        assertThrows(IllegalArgumentException.class,
                () -> Solicitud.crear(proveedor, publicacion, franja.getId(), "", "Objetivo"));
        assertThrows(IllegalArgumentException.class,
                () -> Solicitud.crear(cliente, publicacion, UUID.randomUUID(), "", "Objetivo"));
        assertThrows(IllegalArgumentException.class,
                () -> Solicitud.crear(cliente, publicacion, franja.getId(), "", " "));

        publicacion.pausar("proveedor");
        assertThrows(IllegalStateException.class,
                () -> Solicitud.crear(cliente, publicacion, franja.getId(), "", "Objetivo"));
    }
}
