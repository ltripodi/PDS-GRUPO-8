package com.skillswap;

import com.skillswap.application.busqueda.BusquedaService;
import com.skillswap.application.busqueda.RecomendacionService;
import com.skillswap.application.calificacion.CalificacionRepository;
import com.skillswap.application.calificacion.CalificacionService;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.contratacion.ContratacionService;
import com.skillswap.application.contratacion.SolicitudRepository;
import com.skillswap.application.notificacion.NotificacionesObserver;
import com.skillswap.application.notificacion.NotificacionRepository;
import com.skillswap.application.notificacion.NotificacionService;
import com.skillswap.application.pagos.BilleteraRepository;
import com.skillswap.application.pagos.PagoService;
import com.skillswap.application.pagos.WalletService;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.perfil.PerfilService;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.application.publicacion.PublicacionService;
import com.skillswap.domain.busqueda.RecomendacionPorHabilidades;
import com.skillswap.domain.busqueda.RecomendacionPorPopularidad;
import com.skillswap.domain.contratacion.PoliticaPorAnticipacion;
import com.skillswap.infrastructure.adapters.notificacion.CanalInterno;
import com.skillswap.infrastructure.adapters.notificacion.EmailExternoMock;
import com.skillswap.infrastructure.adapters.notificacion.EmailMockAdapter;
import com.skillswap.infrastructure.adapters.notificacion.PushExternoMock;
import com.skillswap.infrastructure.adapters.notificacion.PushMockAdapter;
import com.skillswap.infrastructure.evento.PublicadorEventosEnMemoria;
import com.skillswap.infrastructure.persistence.JpaBilleteraRepository;
import com.skillswap.infrastructure.persistence.JpaCalificacionRepository;
import com.skillswap.infrastructure.persistence.JpaContratacionRepository;
import com.skillswap.infrastructure.persistence.JpaHabilidadRepository;
import com.skillswap.infrastructure.persistence.JpaNotificacionRepository;
import com.skillswap.infrastructure.persistence.JpaOrganizacionRepository;
import com.skillswap.infrastructure.persistence.JpaPagoRepository;
import com.skillswap.infrastructure.persistence.JpaPerfilRepository;
import com.skillswap.infrastructure.persistence.JpaPromocionRepository;
import com.skillswap.infrastructure.persistence.JpaPublicacionRepository;
import com.skillswap.infrastructure.persistence.JpaSolicitudRepository;
import com.skillswap.infrastructure.persistence.JpaUnidadTransaccional;
import com.skillswap.infrastructure.persistence.JPAUtil;
import com.skillswap.presentation.consola.ConsolaSkillSwap;
import com.skillswap.presentation.contrato.Controladores;
import com.skillswap.presentation.controller.BusquedaController;
import com.skillswap.presentation.controller.CalificacionController;
import com.skillswap.presentation.controller.ContratacionController;
import com.skillswap.presentation.controller.NotificacionController;
import com.skillswap.presentation.controller.PerfilController;
import com.skillswap.presentation.controller.PublicacionController;
import com.skillswap.presentation.controller.RecomendacionController;
import com.skillswap.presentation.controller.WalletController;

import com.skillswap.presentation.ui.SkillSwapFrame;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import java.awt.GraphicsEnvironment;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/*
 * Punto de entrada y composition root: el unico lugar que conoce las implementaciones concretas.
 * Arma repositorios JPA -> servicios -> controllers, conecta el Observer de notificaciones,
 * elige la politica de cancelacion y arranca la interfaz, que solo recibe los controllers.
 */
public final class SkillSwapApplication implements AutoCloseable {

    /* Politica de cancelacion de la demo: reintegro total con 48 h de anticipacion, 50 % con 24 h, nada despues. */
    private static final Duration ANTICIPACION_REINTEGRO_TOTAL = Duration.ofHours(48);
    private static final Duration ANTICIPACION_REINTEGRO_PARCIAL = Duration.ofHours(24);
    /* Ventana de la recomendacion por popularidad: solicitudes de los ultimos 30 dias. */
    private static final Duration VENTANA_POPULARIDAD = Duration.ofDays(30);

    private final Controladores controladores;

    /*
     * Por defecto abre la ventana Swing. Con el argumento --consola (o sin pantalla) usa la consola.
     * Como en el ejemplo MagicCafe, se conecta antes de abrir la ventana y fuera del hilo de Swing.
     */
    public static void main(String[] args) {
        boolean consola = Arrays.asList(args).contains("--consola") || GraphicsEnvironment.isHeadless();
        System.out.println("Conectando con la base y aplicando migraciones (Flyway)...");
        SkillSwapApplication app;
        try {
            app = new SkillSwapApplication();
        } catch (RuntimeException | ExceptionInInitializerError e) {
            if (consola) {
                throw e;
            }
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, "No se pudo conectar con MySQL en localhost:3306.\n"
                            + "Revisá que el servicio esté corriendo y el usuario/clave (ver README).\n\n"
                            + causaRaiz(e).getMessage(),
                    "SkillSwap", JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (consola) {
            try (app) {
                new ConsolaSkillSwap(app.controladores(), new Scanner(System.in)).ejecutar();
            }
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(app::close));
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
                // Si no se puede, queda el look and feel por defecto de Swing.
            }
            SkillSwapFrame ventana = new SkillSwapFrame(app.controladores());
            ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            ventana.setVisible(true);
        });
    }

    private static Throwable causaRaiz(Throwable e) {
        Throwable causa = e;
        while (causa.getCause() != null && causa.getCause() != causa) {
            causa = causa.getCause();
        }
        return causa;
    }

    public SkillSwapApplication() {
        // Conecta ya (y corre Flyway): si MySQL no esta, falla aca y no con la ventana abierta.
        JPAUtil.consultar(em -> Boolean.TRUE);

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
        EmailExternoMock email = new EmailExternoMock();
        PushExternoMock push = new PushExternoMock();
        PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
        NotificacionService notificacionService = new NotificacionService(notificacionRepo, perfilRepo,
                List.of(new CanalInterno(notificacionRepo), new EmailMockAdapter(email), new PushMockAdapter(push)));
        publicador.suscribir(new NotificacionesObserver(notificacionService));

        // Servicios (casos de uso)
        PagoService pagoService = new PagoService(contratacionRepo, billeteraRepo, perfilRepo, organizacionRepo,
                unidad, null, publicador, new JpaPagoRepository());
        // Strategy: la politica de cancelacion decide el reintegro.
        ContratacionService contratacionService = new ContratacionService(contratacionRepo, publicacionRepo,
                perfilRepo, organizacionRepo, unidad, publicador, pagoService,
                new PoliticaPorAnticipacion(ANTICIPACION_REINTEGRO_TOTAL, ANTICIPACION_REINTEGRO_PARCIAL),
                new JpaPromocionRepository());
        // Strategy: cada criterio de recomendacion es una estrategia intercambiable.
        RecomendacionService recomendacionService = new RecomendacionService(
                List.of(new RecomendacionPorHabilidades(), new RecomendacionPorPopularidad()),
                publicacionRepo, perfilRepo, solicitudRepo, VENTANA_POPULARIDAD);

        // Controllers (adaptadores de entrada): lo unico que recibe la interfaz.
        controladores = new Controladores(
                new BusquedaController(new BusquedaService(publicacionRepo, perfilRepo)),
                new RecomendacionController(recomendacionService),
                new ContratacionController(contratacionService, pagoService),
                new CalificacionController(
                        new CalificacionService(calificacionRepo, contratacionRepo, perfilRepo, unidad)),
                new PerfilController(new PerfilService(perfilRepo, new JpaHabilidadRepository(),
                        calificacionRepo, publicacionRepo, contratacionRepo, unidad)),
                new PublicacionController(new PublicacionService(publicacionRepo, perfilRepo, organizacionRepo)),
                new WalletController(new WalletService(billeteraRepo, unidad)),
                new NotificacionController(notificacionService),
                () -> email.getEnviados().size(),
                () -> push.getEmitidos().size());
    }

    public Controladores controladores() {
        return controladores;
    }

    @Override
    public void close() {
        JPAUtil.close();
    }
}
