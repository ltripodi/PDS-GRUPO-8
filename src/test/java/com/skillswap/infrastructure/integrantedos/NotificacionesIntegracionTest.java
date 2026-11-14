package com.skillswap.infrastructure.integrantedos;

import com.skillswap.application.ContratacionDTO;
import com.skillswap.application.ContratacionService;
import com.skillswap.application.DatosSolicitudDTO;
import com.skillswap.application.NotificacionDTO;
import com.skillswap.application.NotificacionService;
import com.skillswap.application.NotificacionesObserver;
import com.skillswap.application.Pagina;
import com.skillswap.application.PagoService;
import com.skillswap.application.PerfilRepository;
import com.skillswap.application.UnidadTransaccional;
import com.skillswap.controllers.NotificacionController;
import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.PoliticaSinReintegro;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.TipoEvento;
import com.skillswap.domain.TipoPublicacion;
import com.skillswap.infrastructure.CanalInterno;
import com.skillswap.infrastructure.EmailExternoMock;
import com.skillswap.infrastructure.EmailMockAdapter;
import com.skillswap.infrastructure.JPAUtil;
import com.skillswap.infrastructure.JpaContratacionRepository;
import com.skillswap.infrastructure.JpaNotificacionRepository;
import com.skillswap.infrastructure.JpaOrganizacionRepository;
import com.skillswap.infrastructure.JpaPerfilRepository;
import com.skillswap.infrastructure.JpaPublicacionRepository;
import com.skillswap.infrastructure.JpaUnidadTransaccional;
import com.skillswap.infrastructure.PublicadorEventosEnMemoria;
import com.skillswap.infrastructure.PushExternoMock;
import com.skillswap.infrastructure.PushMockAdapter;
import jakarta.persistence.EntityManager;
import org.hibernate.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.ResultSet;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Punto 4.6 de punta a punta sobre H2: un caso de uso real publica el evento (Observer), el observador
 * lo pasa a NotificacionService, los adapters mock lo entregan y la bandeja queda guardada en la base.
 * Cada test usa usuarios propios, asi no depende de los datos de otros tests.
 * No se cierra JPAUtil aca: lo cierra PersistenciaTest.
 */
@DisplayName("4.6 Notificaciones contra JPA")
class NotificacionesIntegracionTest {

    private final PerfilRepository perfiles = new JpaPerfilRepository();
    private final JpaNotificacionRepository notificaciones = new JpaNotificacionRepository();
    private final UnidadTransaccional unidad = new JpaUnidadTransaccional();

    private final EmailExternoMock email = new EmailExternoMock();
    private final PushExternoMock push = new PushExternoMock();
    private final PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
    private final NotificacionService notificacionService = new NotificacionService(notificaciones, perfiles,
            List.of(new CanalInterno(notificaciones), new EmailMockAdapter(email), new PushMockAdapter(push)));
    private final NotificacionController controller = new NotificacionController(notificacionService);

    /* El caso de uso de 4.4 sin modificar: solo se le inyecta el publicador real. Pagos no interviene. */
    private final ContratacionService contrataciones = new ContratacionService(new JpaContratacionRepository(),
            new JpaPublicacionRepository(), perfiles, new JpaOrganizacionRepository(), unidad, publicador,
            new PagoService(), new PoliticaSinReintegro());

    NotificacionesIntegracionTest() {
        publicador.suscribir(new NotificacionesObserver(notificacionService));
    }

    /**
     * Caso de aceptacion 8: al aceptar una solicitud, el cliente recibe la notificacion interna (guardada en la
     * base) y la de email, mediante adaptadores mock.
     */
    @Test
    void alAceptarUnaSolicitudElClienteRecibeLaNotificacionInternaYLaDeEmail() {
        Perfil cliente = nuevoPerfil(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL);
        Perfil proveedor = nuevoPerfil(CanalNotificacion.INTERNA);
        ContratacionDTO contratacion = solicitar(cliente, nuevaPublicacion(proveedor));

        contrataciones.aceptar(proveedor.getUserId(), contratacion.getId());

        assertEquals(List.of("Tu solicitud fue aceptada", "Tu solicitud fue enviada"), titulos(cliente));
        assertTrue(email.getEnviados().contains(new EmailExternoMock.CorreoEnviado(cliente.getUserId(),
                "Tu solicitud fue aceptada\n\nConfirma el pago con SkillCoins para reservar la sesion.")));
        assertTrue(push.getEmitidos().isEmpty());
    }

    @Test
    void unaSolicitudNotificaALasDosPartesConTextosDistintos() {
        Perfil cliente = nuevoPerfil(CanalNotificacion.INTERNA);
        Perfil proveedor = nuevoPerfil(CanalNotificacion.INTERNA, CanalNotificacion.PUSH);

        solicitar(cliente, nuevaPublicacion(proveedor));

        assertEquals(List.of("Tu solicitud fue enviada"), titulos(cliente));
        assertEquals(List.of("Recibiste una solicitud"), titulos(proveedor));
        assertEquals(List.of(new PushExternoMock.PushEmitido(proveedor.getUserId(), "Recibiste una solicitud")),
                push.getEmitidos());
    }

    /* Decision de diseño: toda notificacion queda en la bandeja; la preferencia elige los canales externos. */
    @Test
    void quienEligioSoloEmailIgualTieneLaNotificacionGuardadaEnSuBandeja() {
        Perfil cliente = nuevoPerfil(CanalNotificacion.EMAIL);
        Perfil proveedor = nuevoPerfil(CanalNotificacion.INTERNA);

        solicitar(cliente, nuevaPublicacion(proveedor));

        assertEquals(List.of("Tu solicitud fue enviada"), titulos(cliente));
        assertEquals(1, email.getEnviados().size());
    }

    /*
     * Contrato con el modulo de pagos (integrante 4): PagoService.pagar publica PAGO_CONFIRMADO dentro de su
     * transaccion. Se publica igual que lo hara el, para probar que el 4.6 ya lo procesa de punta a punta.
     */
    @Test
    void elPagoConfirmadoQuePublicaPagosLlegaALasDosPartes() {
        Perfil cliente = nuevoPerfil(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL);
        Perfil proveedor = nuevoPerfil(CanalNotificacion.INTERNA);

        unidad.ejecutar(() -> publicador.publicarDespuesDelCommit(new EventoDominio(TipoEvento.PAGO_CONFIRMADO,
                UUID.randomUUID(), cliente.getUserId(), Set.of(cliente.getUserId(), proveedor.getUserId()))));

        assertEquals(List.of("Tu pago fue confirmado"), titulos(cliente));
        assertEquals(List.of("El cliente pago la sesion"), titulos(proveedor));
        assertEquals(1, email.getEnviados().size());
    }

    /* Si el caso de uso falla y hace rollback, el evento se descarta: no hay notificacion de algo que no paso. */
    @Test
    void unaOperacionRechazadaNoGeneraNotificaciones() {
        Perfil cliente = nuevoPerfil(CanalNotificacion.INTERNA);
        Perfil proveedor = nuevoPerfil(CanalNotificacion.INTERNA);
        ContratacionDTO contratacion = solicitar(cliente, nuevaPublicacion(proveedor));
        contrataciones.aceptar(proveedor.getUserId(), contratacion.getId());

        assertThrows(RuntimeException.class,
                () -> contrataciones.aceptar(proveedor.getUserId(), contratacion.getId()));

        assertEquals(List.of("Tu solicitud fue aceptada", "Tu solicitud fue enviada"), titulos(cliente));
    }

    @Test
    void laBandejaSePaginaEnLaBaseYMarcarLeidaQuedaGuardado() {
        String usuario = "user-" + UUID.randomUUID();
        for (int i = 0; i < 5; i++) {
            notificaciones.guardar(new com.skillswap.domain.Notificacion(usuario, TipoEvento.ACEPTACION, "n" + i, "d"));
        }

        Pagina<NotificacionDTO> primera = controller.listar(usuario, 0, 2);
        Pagina<NotificacionDTO> ultima = controller.listar(usuario, 2, 2);
        Set<UUID> recorridas = new HashSet<>();
        for (int numero = 0; numero < 3; numero++) {
            controller.listar(usuario, numero, 2).getElementos()
                    .forEach(dto -> recorridas.add(dto.getId()));
        }

        assertEquals(2, primera.getElementos().size());
        assertEquals(1, ultima.getElementos().size());
        assertEquals(5, primera.getTotalElementos());
        assertEquals(5, recorridas.size());

        UUID id = primera.getElementos().get(0).getId();
        controller.marcarLeida(usuario, id.toString());
        assertTrue(notificaciones.buscarPorId(id).orElseThrow().isLeida());

        controller.marcarTodasLeidas(usuario);
        assertTrue(controller.listar(usuario).getElementos().stream().allMatch(NotificacionDTO::isLeida));
        assertThrows(SecurityException.class, () -> controller.marcarLeida("otro-usuario", id.toString()));
    }

    @Test
    void laTablaDeNotificacionesTieneElIndicePorDestinatario() throws Exception {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            Set<String> indices = new HashSet<>();
            em.unwrap(Session.class).doWork(conexion -> {
                try (ResultSet filas = conexion.getMetaData()
                        .getIndexInfo(null, null, "NOTIFICACIONES", false, false)) {
                    while (filas.next()) {
                        String nombre = filas.getString("INDEX_NAME");
                        if (nombre != null) {
                            indices.add(nombre.toLowerCase());
                        }
                    }
                }
            });
            assertTrue(indices.contains("idx_notificaciones_destinatario_fecha"), "Indices: " + indices);
        } finally {
            em.close();
        }
    }

    // ---------- helpers ----------

    private Perfil nuevoPerfil(CanalNotificacion... canales) {
        Perfil perfil = new Perfil("user-" + UUID.randomUUID(), "Ana", "", ZoneId.of("UTC"));
        perfil.cambiarCanalesNotificacion(Set.of(canales));
        perfiles.guardar(perfil);
        return perfil;
    }

    private Publicacion nuevaPublicacion(Perfil proveedor) {
        Categoria categoria = new Categoria("cat-" + UUID.randomUUID());
        JPAUtil.transaccion(em -> {
            em.persist(categoria);
            return null;
        });
        Publicacion publicacion = Publicacion.crearOferta(proveedor, TipoPublicacion.MENTORIA, "Mentoria", "desc",
                categoria, Modalidad.REMOTA, new SkillCoins(50), 60, 2);
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS);
        publicacion.agregarFranja(new FranjaHoraria(inicio, inicio.plus(1, ChronoUnit.HOURS), 2));
        publicacion.publicar(proveedor.getUserId());
        new JpaPublicacionRepository().guardar(publicacion);
        return publicacion;
    }

    private ContratacionDTO solicitar(Perfil cliente, Publicacion publicacion) {
        return contrataciones.solicitar(cliente.getUserId(), new DatosSolicitudDTO(publicacion.getId(),
                publicacion.getFranjas().get(0).getId(), "Hola", "Aprender"));
    }

    /* La bandeja viene de la mas reciente a la mas vieja. */
    private List<String> titulos(Perfil perfil) {
        List<NotificacionDTO> bandeja = controller.listar(perfil.getUserId()).getElementos();
        assertFalse(bandeja.stream().anyMatch(NotificacionDTO::isLeida));
        return bandeja.stream().map(NotificacionDTO::getTitulo).toList();
    }
}