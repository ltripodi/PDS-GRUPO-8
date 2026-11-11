package com.skillswap.application;

import com.skillswap.controllers.ContratacionController;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.EstadoContratacionBase;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.PoliticaCancelacion;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.Solicitud;
import com.skillswap.domain.TipoEvento;
import com.skillswap.domain.TipoPublicacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/* Integrante 3 (4.4 y 4.5): dominio (State) y casos de uso con puertos simulados. */
class IntegranteTresTest {

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

    // ---------- dominio: State ----------

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

    // ---------- dominio: reserva y cancelacion ----------

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

    // ---------- dominio: reprogramacion ----------

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
    void noSePuedeReprogramarUnaSesionEnCurso() {
        Contratacion contratacion = nuevaContratacion(franja);
        contratacion.aceptar(proveedor.getId());
        contratacion.confirmarPago(cliente.getId());
        contratacion.iniciar(proveedor.getId());

        assertThrows(IllegalStateException.class,
                () -> contratacion.proponerReprogramacion(cliente.getId(), otraFranja));
    }

    // ---------- dominio: solicitud ----------

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

    // ---------- casos de uso ----------

    @Test
    void servicioSolicitaAceptaYPublicaLosEventos() {
        Escenario escenario = new Escenario(politicaFija(100));

        ContratacionDTO dto = escenario.servicio.solicitar("cliente",
                new DatosSolicitudDTO(publicacion.getId(), franja.getId(), "Hola", "Aprender"));
        escenario.servicio.aceptar("proveedor", dto.getId());

        assertEquals(EstadoContratacionDTO.PENDIENTE, dto.getEstado());
        assertEquals("Proveedor", dto.getContraparte());
        assertEquals("ACEPTADA", escenario.contrataciones.buscarPorId(dto.getId()).orElseThrow().getEstado());
        assertEquals(List.of(TipoEvento.SOLICITUD_RECIBIDA, TipoEvento.ACEPTACION), escenario.eventos.tipos());
        assertTrue(escenario.eventos.publicados.get(0).getDestinatarios().containsAll(List.of("cliente", "proveedor")));
    }

    @Test
    void servicioRechazaUnUsuarioQueNoParticipa() {
        Escenario escenario = new Escenario(politicaFija(100));
        escenario.perfiles.guardar(new Perfil("intruso", "Intruso", "", ZoneId.of("UTC")));
        ContratacionDTO dto = escenario.servicio.solicitar("cliente",
                new DatosSolicitudDTO(publicacion.getId(), franja.getId(), "", "Aprender"));

        assertThrows(SecurityException.class, () -> escenario.servicio.aceptar("intruso", dto.getId()));
    }

    @Test
    void cancelarUnaContratacionPagadaReintegraSegunLaPoliticaConfigurada() {
        assertEquals(100, reintegroAlCancelarCon(politicaFija(100)));
        assertEquals(0, reintegroAlCancelarCon(politicaFija(0)));
    }

    private int reintegroAlCancelarCon(PoliticaCancelacion politica) {
        prepararPublicacionPublicada();
        Escenario escenario = new Escenario(politica);
        ContratacionDTO dto = escenario.servicio.solicitar("cliente",
                new DatosSolicitudDTO(publicacion.getId(), franja.getId(), "", "Aprender"));
        escenario.servicio.aceptar("proveedor", dto.getId());
        // Simula el efecto de PagoService (integrante 4).
        escenario.contrataciones.buscarPorId(dto.getId()).orElseThrow().confirmarPago(cliente.getId());

        escenario.servicio.cancelar("cliente", dto.getId(), "Imprevisto");

        return escenario.pagos.reintegrado;
    }

    @Test
    void controllerValidaLaEntradaAntesDeDelegar() {
        Escenario escenario = new Escenario(politicaFija(100));
        ContratacionController controller = new ContratacionController(escenario.servicio, escenario.pagos);

        assertThrows(IllegalArgumentException.class, () -> controller.aceptar(" ", UUID.randomUUID()));
        assertThrows(IllegalArgumentException.class, () -> controller.cancelar("cliente", UUID.randomUUID(), ""));

        UUID id = controller.solicitar("cliente", publicacion.getId(), "Hola", franja.getId(), "Aprender");
        Pagina<ContratacionDTO> pagina = controller.listarContratacionesPaginado("cliente", 0, 10);

        assertEquals(1, pagina.getTotalElementos());
        assertEquals(id, pagina.getElementos().get(0).getId());
    }

    // ---------- puertos simulados ----------

    private static PoliticaCancelacion politicaFija(int porcentaje) {
        return (total, anticipacion) -> new SkillCoins(total.getValor() * porcentaje / 100);
    }

    private final class Escenario {
        final InMemoryPerfilRepository perfiles = new InMemoryPerfilRepository();
        final InMemoryContratacionRepository contrataciones = new InMemoryContratacionRepository(perfiles);
        final EventosEnMemoria eventos = new EventosEnMemoria();
        final PagoServiceFalso pagos = new PagoServiceFalso();
        final ContratacionService servicio;

        Escenario(PoliticaCancelacion politica) {
            perfiles.guardar(cliente);
            perfiles.guardar(proveedor);
            servicio = new ContratacionService(contrataciones, new UnaPublicacionRepository(publicacion), perfiles,
                    id -> Optional.empty(), Runnable::run, eventos, pagos, politica);
        }
    }

    private static final class PagoServiceFalso extends PagoService {
        int reintegrado;

        @Override
        public void realizarReintegro(UUID contratacionId, SkillCoins monto) {
            reintegrado += monto.getValor();
        }
    }

    private static final class EventosEnMemoria implements PublicadorEventosPort {
        final List<EventoDominio> publicados = new ArrayList<>();

        @Override
        public void publicarDespuesDelCommit(EventoDominio evento) {
            publicados.add(evento);
        }

        @Override
        public void suscribir(com.skillswap.domain.ObservadorEvento obs) {
        }

        List<TipoEvento> tipos() {
            return publicados.stream().map(EventoDominio::getTipo).toList();
        }
    }

    private static final class InMemoryPerfilRepository implements PerfilRepository {
        private final Map<String, Perfil> perfiles = new HashMap<>();

        @Override
        public Optional<Perfil> buscarPorUserId(String userId) {
            return Optional.ofNullable(perfiles.get(userId));
        }

        @Override
        public Optional<Perfil> buscarPorId(UUID id) {
            return perfiles.values().stream().filter(perfil -> perfil.getId().equals(id)).findFirst();
        }

        @Override
        public void guardar(Perfil perfil) {
            perfiles.put(perfil.getUserId(), perfil);
        }
    }

    private static final class InMemoryContratacionRepository implements ContratacionRepository {
        private final Map<UUID, Contratacion> contrataciones = new HashMap<>();
        private final InMemoryPerfilRepository perfiles;

        private InMemoryContratacionRepository(InMemoryPerfilRepository perfiles) {
            this.perfiles = perfiles;
        }

        @Override
        public Optional<Contratacion> buscarPorId(UUID id) {
            return Optional.ofNullable(contrataciones.get(id));
        }

        @Override
        public List<Contratacion> listarPorParticipante(String userId) {
            UUID perfilId = perfiles.buscarPorUserId(userId).map(Perfil::getId).orElse(null);
            return contrataciones.values().stream().filter(c -> c.esParticipante(perfilId)).toList();
        }

        @Override
        public void guardar(Contratacion contratacion) {
            contrataciones.put(contratacion.getId(), contratacion);
        }
    }

    /* Solo implementa lo que usa ContratacionService. */
    private static final class UnaPublicacionRepository implements PublicacionRepository {
        private final Publicacion publicacion;

        private UnaPublicacionRepository(Publicacion publicacion) {
            this.publicacion = publicacion;
        }

        @Override
        public Optional<Publicacion> buscarPorId(UUID id) {
            return publicacion.getId().equals(id) ? Optional.of(publicacion) : Optional.empty();
        }

        @Override
        public void guardar(Publicacion publicacion) {
        }

        @Override
        public List<Publicacion> buscarCandidatas() {
            return List.of(publicacion);
        }

        @Override
        public List<Publicacion> buscarActivasPorAutor(String userId) {
            return List.of();
        }

        @Override
        public List<Publicacion> listarPropias(String userId) {
            return List.of();
        }

        @Override
        public Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Categoria> listarCategorias() {
            return List.of();
        }

        @Override
        public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
            throw new UnsupportedOperationException();
        }
    }
}
