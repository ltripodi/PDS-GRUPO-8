package com.skillswap.presentation;

import com.skillswap.application.BilleteraRepository;
import com.skillswap.application.BusquedaService;
import com.skillswap.application.CalificacionRepository;
import com.skillswap.application.CalificacionService;
import com.skillswap.application.ContratacionRepository;
import com.skillswap.application.ContratacionService;
import com.skillswap.application.NotificacionRepository;
import com.skillswap.application.NotificacionService;
import com.skillswap.application.NotificacionesObserver;
import com.skillswap.application.OrganizacionRepository;
import com.skillswap.application.PagoService;
import com.skillswap.application.PerfilRepository;
import com.skillswap.application.PerfilService;
import com.skillswap.application.PublicacionRepository;
import com.skillswap.application.PublicacionService;
import com.skillswap.application.RecomendacionService;
import com.skillswap.application.SolicitudRepository;
import com.skillswap.application.UnidadTransaccional;
import com.skillswap.application.WalletService;
import com.skillswap.controllers.BusquedaController;
import com.skillswap.controllers.CalificacionController;
import com.skillswap.controllers.ContratacionController;
import com.skillswap.controllers.NotificacionController;
import com.skillswap.controllers.PerfilController;
import com.skillswap.controllers.PublicacionController;
import com.skillswap.controllers.RecomendacionController;
import com.skillswap.controllers.WalletController;
import com.skillswap.domain.PoliticaPorAnticipacion;
import com.skillswap.domain.RecomendacionPorHabilidades;
import com.skillswap.domain.RecomendacionPorPopularidad;
import com.skillswap.infrastructure.CanalInterno;
import com.skillswap.infrastructure.EmailExternoMock;
import com.skillswap.infrastructure.EmailMockAdapter;
import com.skillswap.infrastructure.JPAUtil;
import com.skillswap.infrastructure.JpaBilleteraRepository;
import com.skillswap.infrastructure.JpaCalificacionRepository;
import com.skillswap.infrastructure.JpaContratacionRepository;
import com.skillswap.infrastructure.JpaHabilidadRepository;
import com.skillswap.infrastructure.JpaNotificacionRepository;
import com.skillswap.infrastructure.JpaOrganizacionRepository;
import com.skillswap.infrastructure.JpaPagoRepository;
import com.skillswap.infrastructure.JpaPerfilRepository;
import com.skillswap.infrastructure.JpaPromocionRepository;
import com.skillswap.infrastructure.JpaPublicacionRepository;
import com.skillswap.infrastructure.JpaSolicitudRepository;
import com.skillswap.infrastructure.JpaUnidadTransaccional;
import com.skillswap.infrastructure.PublicadorEventosEnMemoria;
import com.skillswap.infrastructure.PushExternoMock;
import com.skillswap.infrastructure.PushMockAdapter;

import java.time.Duration;
import java.util.List;

/*
 * Composition root: el unico lugar que conoce las implementaciones concretas.
 * Arma repositorios JPA -> servicios -> controllers, conecta el Observer de notificaciones
 * y elige la politica de cancelacion. La interfaz (ConsolaSkillSwap) solo usa los controllers.
 */
public final class ContenedorAplicacion implements AutoCloseable {

    /* Politica de cancelacion de la demo: reintegro total con 48 h de anticipacion, 50 % con 24 h, nada despues. */
    private static final Duration ANTICIPACION_REINTEGRO_TOTAL = Duration.ofHours(48);
    private static final Duration ANTICIPACION_REINTEGRO_PARCIAL = Duration.ofHours(24);
    /* Ventana de la recomendacion por popularidad: solicitudes de los ultimos 30 dias. */
    private static final Duration VENTANA_POPULARIDAD = Duration.ofDays(30);

    private final EmailExternoMock email = new EmailExternoMock();
    private final PushExternoMock push = new PushExternoMock();

    private final BusquedaController busqueda;
    private final RecomendacionController recomendaciones;
    private final ContratacionController contrataciones;
    private final CalificacionController calificaciones;
    private final PerfilController perfiles;
    private final PublicacionController publicaciones;
    private final WalletController billetera;
    private final NotificacionController notificaciones;

    public ContenedorAplicacion() {
        // Repositorios (adaptadores de salida)
        PerfilRepository perfilRepo = new JpaPerfilRepository();
        PublicacionRepository publicacionRepo = new JpaPublicacionRepository();
        ContratacionRepository contratacionRepo = new JpaContratacionRepository();
        OrganizacionRepository organizacionRepo = new JpaOrganizacionRepository();
        BilleteraRepository billeteraRepo = new JpaBilleteraRepository();
        CalificacionRepository calificacionRepo = new JpaCalificacionRepository();
        SolicitudRepository solicitudRepo = new JpaSolicitudRepository();
        NotificacionRepository notificacionRepo = new JpaNotificacionRepository();
        UnidadTransaccional unidad = new JpaUnidadTransaccional();

        // Observer: los casos de uso publican eventos; el observador los convierte en notificaciones.
        // Adapter: email y push son servicios externos simulados detras de CanalEnvio.
        PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
        NotificacionService notificacionService = new NotificacionService(notificacionRepo, perfilRepo,
                List.of(new CanalInterno(notificacionRepo), new EmailMockAdapter(email), new PushMockAdapter(push)));
        publicador.suscribir(new NotificacionesObserver(notificacionService));

        // Servicios (casos de uso)
        PagoService pagoService = new PagoService(contratacionRepo, billeteraRepo, perfilRepo, organizacionRepo,
                unidad, null, publicador, new JpaPagoRepository());
        ContratacionService contratacionService = new ContratacionService(contratacionRepo, publicacionRepo,
                perfilRepo, organizacionRepo, unidad, publicador, pagoService,
                new PoliticaPorAnticipacion(ANTICIPACION_REINTEGRO_TOTAL, ANTICIPACION_REINTEGRO_PARCIAL),
                new JpaPromocionRepository());
        // Strategy: cada criterio de recomendacion es una estrategia intercambiable.
        RecomendacionService recomendacionService = new RecomendacionService(
                List.of(new RecomendacionPorHabilidades(), new RecomendacionPorPopularidad()),
                publicacionRepo, perfilRepo, solicitudRepo, VENTANA_POPULARIDAD);

        // Controllers (adaptadores de entrada)
        busqueda = new BusquedaController(new BusquedaService(publicacionRepo, perfilRepo));
        recomendaciones = new RecomendacionController(recomendacionService);
        contrataciones = new ContratacionController(contratacionService, pagoService);
        calificaciones = new CalificacionController(
                new CalificacionService(calificacionRepo, contratacionRepo, perfilRepo, unidad));
        perfiles = new PerfilController(new PerfilService(perfilRepo, new JpaHabilidadRepository(),
                calificacionRepo, publicacionRepo, contratacionRepo, unidad));
        publicaciones = new PublicacionController(
                new PublicacionService(publicacionRepo, perfilRepo, organizacionRepo));
        billetera = new WalletController(new WalletService(billeteraRepo, unidad));
        notificaciones = new NotificacionController(notificacionService);
    }

    public BusquedaController busqueda() {
        return busqueda;
    }

    public RecomendacionController recomendaciones() {
        return recomendaciones;
    }

    public ContratacionController contrataciones() {
        return contrataciones;
    }

    public CalificacionController calificaciones() {
        return calificaciones;
    }

    public PerfilController perfiles() {
        return perfiles;
    }

    public PublicacionController publicaciones() {
        return publicaciones;
    }

    public WalletController billetera() {
        return billetera;
    }

    public NotificacionController notificaciones() {
        return notificaciones;
    }

    /* Cuantos emails y push "salieron" por los mocks: sirve para mostrar el Adapter en la demo. */
    public int emailsEnviados() {
        return email.getEnviados().size();
    }

    public int pushEnviados() {
        return push.getEmitidos().size();
    }

    @Override
    public void close() {
        JPAUtil.close();
    }
}
