package com.skillswap.domain;

import com.skillswap.domain.calificacion.Calificacion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.Solicitud;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/* Reglas de negocio de la seccion 5 que valida el dominio (sin base ni servicios). */
@DisplayName("Reglas de negocio del dominio")
class ReglasDeNegocioTest {

    private final Categoria categoria = new Categoria("Tecnologia");
    private Perfil cliente;
    private Perfil otroCliente;
    private Perfil proveedor;
    private Publicacion publicacion;
    private FranjaHoraria franja;
    private FranjaHoraria otraFranja;

    @BeforeEach
    void prepararPublicacion() {
        cliente = new Perfil("cliente", "Cliente", "", ZoneId.of("UTC"));
        otroCliente = new Perfil("otro", "Otro", "", ZoneId.of("UTC"));
        proveedor = new Perfil("proveedor", "Proveedor", "", ZoneId.of("UTC"));
        publicacion = Publicacion.crearOferta(proveedor, TipoPublicacion.MENTORIA, "Mentoria", "desc", categoria,
                Modalidad.REMOTA, new SkillCoins(100), 60, 1);
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS);
        franja = new FranjaHoraria(inicio, inicio.plus(1, ChronoUnit.HOURS), 1);
        otraFranja = new FranjaHoraria(inicio.plus(1, ChronoUnit.DAYS), inicio.plus(1, ChronoUnit.DAYS).plus(1, ChronoUnit.HOURS), 1);
        publicacion.agregarFranja(franja);
        publicacion.agregarFranja(otraFranja);
        publicacion.publicar(proveedor.getUserId());
    }

    private Contratacion solicitada(Perfil quien, FranjaHoraria elegida) {
        Solicitud solicitud = Solicitud.crear(quien, publicacion, elegida.getId(), "Hola", "Aprender");
        return new Contratacion(quien.getId(), proveedor.getId(), new SkillCoins(100), solicitud);
    }

    // ---------- aceptar ----------

    @Test
    void siNoHayCupoLaContratacionSiguePendienteYSinReserva() {
        Contratacion primera = solicitada(cliente, franja);
        Contratacion segunda = solicitada(otroCliente, franja);
        primera.aceptar(proveedor.getId());

        assertThrows(IllegalStateException.class, () -> segunda.aceptar(proveedor.getId()));

        assertEquals("PENDIENTE", segunda.getEstado());
        assertNull(segunda.getReserva());
        assertEquals(0, segunda.getHistorial().size());
        assertEquals(1, franja.getOcupados());
    }

    @Test
    void noSeAceptaSiLaPublicacionYaNoEstaPublicada() {
        Contratacion contratacion = solicitada(cliente, franja);
        publicacion.pausar(proveedor.getUserId());

        assertThrows(IllegalStateException.class, () -> contratacion.aceptar(proveedor.getId()));

        assertEquals("PENDIENTE", contratacion.getEstado());
        assertEquals(0, franja.getOcupados());
    }

    @Test
    void noSeAceptaSiLaFranjaYaComenzo() throws Exception {
        Instant inicio = Instant.now().plus(150, ChronoUnit.MILLIS);
        FranjaHoraria inminente = new FranjaHoraria(inicio, inicio.plus(1, ChronoUnit.HOURS), 2);
        Publicacion conInminente = Publicacion.crearOferta(proveedor, TipoPublicacion.MENTORIA, "Ya", "desc",
                categoria, Modalidad.REMOTA, new SkillCoins(10), 60, 2);
        conInminente.agregarFranja(inminente);
        conInminente.publicar(proveedor.getUserId());
        Contratacion contratacion = new Contratacion(cliente.getId(), proveedor.getId(), new SkillCoins(10),
                Solicitud.crear(cliente, conInminente, inminente.getId(), "", "Aprender"));

        Thread.sleep(300);

        assertThrows(IllegalStateException.class, () -> contratacion.aceptar(proveedor.getId()));
        assertEquals("PENDIENTE", contratacion.getEstado());
    }

    // ---------- solicitar ----------

    @Test
    void noSeSolicitaUnaFranjaQueYaPaso() {
        Instant ayer = Instant.now().minus(1, ChronoUnit.DAYS);
        FranjaHoraria pasada = new FranjaHoraria(ayer, ayer.plus(1, ChronoUnit.HOURS), 1);
        Publicacion conPasada = Publicacion.crearOferta(proveedor, TipoPublicacion.MENTORIA, "Vieja", "desc",
                categoria, Modalidad.REMOTA, new SkillCoins(10), 60, 1);
        conPasada.agregarFranja(franja);
        conPasada.agregarFranja(pasada);
        conPasada.publicar(proveedor.getUserId());

        assertThrows(IllegalStateException.class,
                () -> Solicitud.crear(cliente, conPasada, pasada.getId(), "", "Aprender"));
    }

    @Test
    void elRepresentanteDeLaOrganizacionNoPuedeSolicitarSuPropioDesafio() {
        Organizacion acme = new Organizacion("Acme", "acme-admin");
        Perfil representante = new Perfil("acme-admin", "Admin de Acme", "", ZoneId.of("UTC"));
        Publicacion desafio = Publicacion.crearDesafio(acme, "Prototipo", "desc", categoria, Modalidad.REMOTA,
                new SkillCoins(100), 480, 3);
        Instant inicio = Instant.now().plus(5, ChronoUnit.DAYS);
        FranjaHoraria franjaDesafio = new FranjaHoraria(inicio, inicio.plus(8, ChronoUnit.HOURS), 3);
        desafio.agregarFranja(franjaDesafio);
        desafio.publicar("acme-admin");

        assertThrows(IllegalArgumentException.class,
                () -> Solicitud.crear(representante, desafio, franjaDesafio.getId(), "", "Ganar"));
        // Cualquier otro perfil si puede postularse.
        Solicitud.crear(cliente, desafio, franjaDesafio.getId(), "", "Ganar");
    }

    // ---------- pagar ----------

    @Test
    void noSePagaUnaContratacionDeUnaPublicacionCancelada() {
        Contratacion contratacion = solicitada(cliente, franja);
        contratacion.aceptar(proveedor.getId());
        publicacion.cancelar(proveedor.getUserId(), "Ya no doy la mentoria");

        assertThrows(IllegalStateException.class, () -> contratacion.confirmarPago(cliente.getId()));
        assertEquals("ACEPTADA", contratacion.getEstado());
    }

    // ---------- reprogramacion ----------

    @Test
    void noSeRechazaUnaReprogramacionDeUnaContratacionCancelada() {
        Contratacion contratacion = solicitada(cliente, franja);
        contratacion.aceptar(proveedor.getId());
        contratacion.proponerReprogramacion(cliente.getId(), otraFranja);
        contratacion.cancelar(cliente.getId(), "No puedo");
        int registros = contratacion.getHistorial().size();

        assertThrows(IllegalStateException.class, () -> contratacion.rechazarReprogramacion(proveedor.getId()));
        assertEquals(registros, contratacion.getHistorial().size(), "no se agrega nada al historial");
    }

    @Test
    void enUnEstadoValidoLaReprogramacionSePuedeRechazar() {
        Contratacion contratacion = solicitada(cliente, franja);
        contratacion.aceptar(proveedor.getId());
        contratacion.proponerReprogramacion(cliente.getId(), otraFranja);

        contratacion.rechazarReprogramacion(proveedor.getId());

        assertEquals(franja.getId(), contratacion.getReserva().getFranjaId());
    }

    // ---------- reputacion ----------

    @Test
    void laReputacionSoloCuentaCalificacionesDirigidasAlPerfil() {
        Calificacion paraElProveedor = new Calificacion(java.util.UUID.randomUUID(), "cliente", "proveedor", 4);
        Calificacion paraOtro = new Calificacion(java.util.UUID.randomUUID(), "cliente", "otro", 1);

        proveedor.recalcularReputacion(List.of(paraElProveedor, paraOtro));

        assertEquals(4.0, proveedor.getReputacion());
    }
}
