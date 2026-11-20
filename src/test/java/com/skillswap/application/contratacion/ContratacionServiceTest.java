package com.skillswap.application.contratacion;

import com.skillswap.application.DoblesEnMemoria;
import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.evento.PublicadorEventosPort;
import com.skillswap.application.pagos.PagoService;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.PoliticaCancelacion;
import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.ObservadorEvento;
import com.skillswap.domain.evento.TipoEvento;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;
import com.skillswap.presentation.controller.ContratacionController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/* Casos de uso de contratacion (4.4 y 4.5) con puertos simulados, incluido su controller. */
class ContratacionServiceTest {

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
    void enUnaContratacionRechazadaSoloSeConsultaElHistorial() {
        Escenario escenario = new Escenario(politicaFija(100));
        escenario.perfiles.guardar(new Perfil("intruso", "Intruso", "", ZoneId.of("UTC")));
        ContratacionController controller = new ContratacionController(escenario.servicio, escenario.pagos);
        UUID id = controller.solicitar("cliente", publicacion.getId(), "Hola", franja.getId(), "Aprender");
        controller.rechazar("proveedor", id);

        assertEquals(List.of(TipoEvento.SOLICITUD_RECIBIDA, TipoEvento.RECHAZO), escenario.eventos.tipos(),
                "el cliente se entera de que lo rechazaron");
        assertThrows(ErrorAplicacion.class, () -> controller.aceptar("proveedor", id));
        List<RegistroHistorialDTO> historial = controller.consultarHistorial("cliente", id).getElementos();

        assertEquals(1, historial.size());
        assertEquals("PENDIENTE", historial.get(0).getOrigen());
        assertEquals("RECHAZADA", historial.get(0).getDestino());
        assertEquals("Proveedor", historial.get(0).getActor());
        assertEquals("Solicitud rechazada por el proveedor", historial.get(0).getMotivo());
        assertThrows(ErrorAplicacion.class, () -> controller.consultarHistorial("intruso", id));
    }

    @Test
    void servicioRechazaUnaPropuestaDeReprogramacion() {
        Escenario escenario = new Escenario(politicaFija(100));
        ContratacionDTO dto = escenario.servicio.solicitar("cliente",
                new DatosSolicitudDTO(publicacion.getId(), franja.getId(), "", "Aprender"));
        escenario.servicio.aceptar("proveedor", dto.getId());
        escenario.servicio.proponerReprogramacion("cliente", dto.getId(), otraFranja.getId());

        escenario.servicio.rechazarReprogramacion("proveedor", dto.getId());

        Contratacion contratacion = escenario.contrataciones.buscarPorId(dto.getId()).orElseThrow();
        assertEquals(franja.getId(), contratacion.getReserva().getFranjaId());
        assertEquals("ACEPTADA", contratacion.getEstado());
        assertEquals(List.of(TipoEvento.SOLICITUD_RECIBIDA, TipoEvento.ACEPTACION,
                TipoEvento.REPROGRAMACION_PROPUESTA, TipoEvento.REPROGRAMACION_RECHAZADA), escenario.eventos.tipos(),
                "la otra parte se entera de la propuesta, y quien propuso de que se descarto");
    }

    @Test
    void cancelarUnaContratacionPagadaReintegraSegunLaPoliticaConfigurada() {
        assertEquals(100, reintegroAlCancelarCon(politicaFija(100), "cliente"));
        assertEquals(0, reintegroAlCancelarCon(politicaFija(0), "cliente"));
    }

    /* La politica configurada es la del cliente: si cancela el proveedor, el cliente recupera todo. */
    @Test
    void siCancelaElProveedorElClienteRecuperaTodoAunqueLaPoliticaNoDevuelvaNada() {
        assertEquals(100, reintegroAlCancelarCon(politicaFija(0), "proveedor"));
    }

    @Test
    void cancelarSinHaberPagadoNoReintegraNada() {
        prepararPublicacionPublicada();
        Escenario escenario = new Escenario(politicaFija(100));
        ContratacionDTO dto = escenario.servicio.solicitar("cliente",
                new DatosSolicitudDTO(publicacion.getId(), franja.getId(), "", "Aprender"));
        escenario.servicio.aceptar("proveedor", dto.getId());

        assertEquals(0, escenario.servicio.cancelar("proveedor", dto.getId(), "No llego"));
        assertEquals(0, escenario.pagos.reintegrado);
    }

    private int reintegroAlCancelarCon(PoliticaCancelacion politica, String quienCancela) {
        prepararPublicacionPublicada();
        Escenario escenario = new Escenario(politica);
        ContratacionDTO dto = escenario.servicio.solicitar("cliente",
                new DatosSolicitudDTO(publicacion.getId(), franja.getId(), "", "Aprender"));
        escenario.servicio.aceptar("proveedor", dto.getId());
        // Simula el efecto de PagoService (integrante 4).
        escenario.contrataciones.buscarPorId(dto.getId()).orElseThrow().confirmarPago(cliente.getId());

        int informado = escenario.servicio.cancelar(quienCancela, dto.getId(), "Imprevisto");

        assertEquals(escenario.pagos.reintegrado, informado, "cancelar informa lo que se reintegro");
        return escenario.pagos.reintegrado;
    }

    @Test
    void controllerValidaLaEntradaAntesDeDelegar() {
        Escenario escenario = new Escenario(politicaFija(100));
        ContratacionController controller = new ContratacionController(escenario.servicio, escenario.pagos);

        assertThrows(ErrorAplicacion.class, () -> controller.aceptar(" ", UUID.randomUUID()));
        assertThrows(ErrorAplicacion.class, () -> controller.cancelar("cliente", UUID.randomUUID(), ""));

        UUID id = controller.solicitar("cliente", publicacion.getId(), "Hola", franja.getId(), "Aprender");
        Pagina<ContratacionDTO> pagina = controller.listarContrataciones("cliente", 0, 10);

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
        final DoblesEnMemoria.Promociones promociones = new DoblesEnMemoria.Promociones();
        final ContratacionService servicio;

        Escenario(PoliticaCancelacion politica) {
            perfiles.guardar(cliente);
            perfiles.guardar(proveedor);
            servicio = new ContratacionService(contrataciones, new UnaPublicacionRepository(publicacion), perfiles,
                    new SinOrganizaciones(), Runnable::run, eventos, pagos, politica, promociones);
        }
    }

    /* Estos escenarios no tienen desafios: no hay organizaciones. */
    private static final class SinOrganizaciones implements OrganizacionRepository {
        @Override
        public Optional<Organizacion> buscarPorId(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Organizacion> buscarPorUserIdAutorizado(String userId) {
            return Optional.empty();
        }
    }

    private static final class PagoServiceFalso extends PagoService {
        int reintegrado;

        PagoServiceFalso() {
            // PagoService ahora recibe sus dependencias; este doble no usa ninguna.
            super(null, null, null, null, null, null, null);
        }

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
        public void suscribir(ObservadorEvento obs) {
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
        public Map<UUID, Perfil> buscarPorIds(Set<UUID> ids) {
            Map<UUID, Perfil> encontrados = new HashMap<>();
            if (ids != null) {
                ids.forEach(id -> buscarPorId(id).ifPresent(perfil -> encontrados.put(id, perfil)));
            }
            return encontrados;
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
        public Pagina<Contratacion> listarPorParticipantePaginado(String userId, int pagina, int tamanio) {
            return Pagina.de(listarPorParticipante(userId), pagina, tamanio);
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
