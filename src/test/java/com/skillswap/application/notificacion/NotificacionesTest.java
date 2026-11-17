package com.skillswap.application.integrantedos;

import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.notificacion.CanalEnvio;
import com.skillswap.application.notificacion.NotificacionDTO;
import com.skillswap.application.notificacion.NotificacionesObserver;
import com.skillswap.application.notificacion.NotificacionService;
import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.ObservadorEvento;
import com.skillswap.domain.evento.TipoEvento;
import com.skillswap.domain.notificacion.CanalNotificacion;
import com.skillswap.domain.notificacion.Notificacion;
import com.skillswap.domain.notificacion.PreferenciaNotificacion;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.infrastructure.notificacion.CanalInterno;
import com.skillswap.infrastructure.notificacion.EmailExternoMock;
import com.skillswap.infrastructure.notificacion.EmailAdapter;
import com.skillswap.infrastructure.notificacion.PushExternoMock;
import com.skillswap.infrastructure.notificacion.PushAdapter;
import com.skillswap.infrastructure.evento.PublicadorEventosEnMemoria;
import com.skillswap.presentation.controller.NotificacionController;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Punto 4.6 de la consigna: notificaciones con Observer (eventos) y Adapter (canales de envio).
 * Tests unitarios con repositorios en memoria; el comportamiento con commit y rollback se prueba
 * contra H2 en PersistenciaTest.
 */
@DisplayName("4.6 Notificaciones")
class NotificacionesTest {

    private static final String CLIENTE = "cliente";
    private static final String PROVEEDOR = "proveedor";

    private final DoblesEnMemoria.Notificaciones bandeja = new DoblesEnMemoria.Notificaciones();
    private final DoblesEnMemoria.Perfiles perfiles = new DoblesEnMemoria.Perfiles();
    private final EmailExternoMock email = new EmailExternoMock();
    private final PushExternoMock push = new PushExternoMock();
    private final NotificacionService servicio = new NotificacionService(bandeja, perfiles,
            List.of(new CanalInterno(bandeja), new EmailAdapter(email), new PushAdapter(push)));

    @Nested
    @DisplayName("Dominio: notificacion y preferencias")
    class Dominio {

        @Test
        void unaNotificacionNaceSinLeerYMarcarlaLeidaEsIdempotente() {
            Notificacion notificacion = new Notificacion("ana", TipoEvento.SOLICITUD_RECIBIDA, "Solicitud recibida", "Detalle");
            assertFalse(notificacion.isLeida());

            notificacion.marcarLeida();
            notificacion.marcarLeida();

            assertTrue(notificacion.isLeida());
            assertEquals("ana", notificacion.getDestinatarioId());
        }

        @Test
        void unaNotificacionExigeDestinatarioTituloYTipo() {
            assertThrows(IllegalArgumentException.class, () -> new Notificacion(null, TipoEvento.ACEPTACION, "Titulo", "d"));
            assertThrows(IllegalArgumentException.class, () -> new Notificacion(" ", TipoEvento.ACEPTACION, "Titulo", "d"));
            assertThrows(IllegalArgumentException.class, () -> new Notificacion("ana", TipoEvento.ACEPTACION, " ", "d"));
            assertThrows(IllegalArgumentException.class, () -> new Notificacion("ana", null, "Titulo", "d"));
            assertEquals("", new Notificacion("ana", TipoEvento.ACEPTACION, "Titulo", null).getDetalle());
        }

        @Test
        void laPreferenciaSoloPermiteLosCanalesElegidosYPorDefectoEsLaBandejaInterna() {
            PreferenciaNotificacion preferencia =
                    new PreferenciaNotificacion(EnumSet.of(CanalNotificacion.EMAIL, CanalNotificacion.PUSH));
            Perfil perfilNuevo = new Perfil("ana", "Ana", "", ZoneId.of("UTC"));

            assertTrue(preferencia.permite(CanalNotificacion.EMAIL));
            assertTrue(preferencia.permite(CanalNotificacion.PUSH));
            assertFalse(preferencia.permite(CanalNotificacion.INTERNA));
            assertFalse(preferencia.permite(null));
            assertTrue(perfilNuevo.getPreferenciaNotificacion().permite(CanalNotificacion.INTERNA));
            assertFalse(perfilNuevo.getPreferenciaNotificacion().permite(CanalNotificacion.EMAIL));
        }

        @Test
        void laPreferenciaEsInmutableYExigeAlMenosUnCanalSinNulos() {
            Set<CanalNotificacion> elegidos = new HashSet<>(Set.of(CanalNotificacion.EMAIL));
            PreferenciaNotificacion preferencia = new PreferenciaNotificacion(elegidos);
            Set<CanalNotificacion> conNulo = new HashSet<>();
            conNulo.add(null);

            elegidos.add(CanalNotificacion.PUSH);

            assertFalse(preferencia.permite(CanalNotificacion.PUSH));
            assertThrows(UnsupportedOperationException.class,
                    () -> preferencia.getCanales().add(CanalNotificacion.PUSH));
            assertTrue(preferencia.permite(CanalNotificacion.EMAIL));
            assertThrows(IllegalArgumentException.class, () -> new PreferenciaNotificacion(null));
            assertThrows(IllegalArgumentException.class, () -> new PreferenciaNotificacion(conNulo));
            assertThrows(IllegalArgumentException.class, () -> new PreferenciaNotificacion(Set.of()));
        }

        /* Value Object: se compara por los canales que permite, no por ser la misma instancia. */
        @Test
        void laPreferenciaSeComparaPorValor() {
            PreferenciaNotificacion emailYPush =
                    new PreferenciaNotificacion(EnumSet.of(CanalNotificacion.EMAIL, CanalNotificacion.PUSH));
            PreferenciaNotificacion mismosCanales =
                    new PreferenciaNotificacion(Set.of(CanalNotificacion.PUSH, CanalNotificacion.EMAIL));
            PreferenciaNotificacion soloEmail = new PreferenciaNotificacion(Set.of(CanalNotificacion.EMAIL));

            assertEquals(emailYPush, mismosCanales);
            assertEquals(emailYPush.hashCode(), mismosCanales.hashCode());
            assertNotEquals(emailYPush, soloEmail);
            assertNotEquals(null, emailYPush);
        }

        @Test
        void unaNotificacionExigeFecha() {
            assertThrows(IllegalArgumentException.class,
                    () -> new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "Titulo", "d", null));
        }
    }

    @Nested
    @DisplayName("Eventos de la consigna: cada parte recibe su mensaje")
    class Eventos {

        @ParameterizedTest(name = "{0} (lo hace el {1})")
        @CsvSource({
                "SOLICITUD_RECIBIDA, cliente,   Tu solicitud fue enviada,           Recibiste una solicitud",
                "ACEPTACION,         proveedor, Aceptaste la solicitud,             Tu solicitud fue aceptada",
                "PAGO_CONFIRMADO,    cliente,   Tu pago fue confirmado,             El cliente pago la sesion",
                "REPROGRAMACION,     cliente,   Confirmaste la reprogramacion,      La sesion fue reprogramada",
                "CANCELACION,        proveedor, Cancelaste la contratacion,         La contratacion fue cancelada",
                "SESION_COMPLETADA,  proveedor, Marcaste la sesion como completada, La sesion fue completada",
                "RECHAZO,            proveedor, Rechazaste la solicitud,            Tu solicitud fue rechazada",
                "REPROGRAMACION_PROPUESTA, cliente, Propusiste una reprogramacion,  Te propusieron reprogramar la sesion",
                "REPROGRAMACION_RECHAZADA, proveedor, Descartaste la propuesta de reprogramacion, La propuesta de reprogramacion fue descartada"
        })
        void notificaAlActorYALaOtraParteConTextosDistintos(TipoEvento tipo, String actor,
                                                            String tituloParaElActor, String tituloParaLaOtraParte) {
            String otraParte = actor.equals(CLIENTE) ? PROVEEDOR : CLIENTE;

            servicio.notificar(eventoEntreLasPartes(tipo, actor));

            Notificacion delActor = unicaNotificacionDe(actor);
            Notificacion deLaOtraParte = unicaNotificacionDe(otraParte);
            assertEquals(tituloParaElActor, delActor.getTitulo());
            assertEquals(tituloParaLaOtraParte, deLaOtraParte.getTitulo());
            assertEquals(tipo, deLaOtraParte.getTipo());
        }

        /* La fecha es la del hecho (el evento), no la del momento en que se proceso el aviso. */
        @Test
        void laNotificacionLlevaLaFechaEnQueOcurrioElEvento() {
            EventoDominio evento = eventoEntreLasPartes(TipoEvento.ACEPTACION, PROVEEDOR);

            servicio.notificar(evento);

            assertEquals(evento.getOcurridoEn(), unicaNotificacionDe(CLIENTE).getFecha());
            assertEquals(evento.getOcurridoEn(), unicaNotificacionDe(PROVEEDOR).getFecha());
        }

        private Notificacion unicaNotificacionDe(String userId) {
            List<Notificacion> recibidas = bandeja.de(userId);
            assertEquals(1, recibidas.size());
            return recibidas.get(0);
        }
    }

    @Nested
    @DisplayName("Canales y preferencias del usuario")
    class Canales {

        /* La preferencia decide los canales externos; la bandeja interna recibe todo (aunque se eligio solo email). */
        @Test
        void losCanalesExternosRespetanLaPreferenciaYLaBandejaRecibeTodo() {
            nuevoPerfil(CLIENTE, CanalNotificacion.EMAIL);
            nuevoPerfil(PROVEEDOR, CanalNotificacion.INTERNA, CanalNotificacion.PUSH);

            servicio.notificar(eventoEntreLasPartes(TipoEvento.ACEPTACION, PROVEEDOR));

            assertEquals(List.of(CLIENTE), email.getEnviados().stream()
                    .map(EmailExternoMock.CorreoEnviado::destinatarioUserId).toList());
            assertEquals(List.of(PROVEEDOR), push.getEmitidos().stream()
                    .map(PushExternoMock.PushEmitido::destinatarioUserId).toList());
            assertEquals(1, bandeja.de(CLIENTE).size());
            assertEquals(1, bandeja.de(PROVEEDOR).size());
        }

        /** Caso de aceptacion 8: dos tipos de notificacion ante un mismo evento mediante adaptadores mock. */
        @Test
        void unMismoEventoSeEnviaPorEmailYPorPushMedianteLosAdaptadores() {
            nuevoPerfil(CLIENTE, CanalNotificacion.EMAIL, CanalNotificacion.PUSH);

            servicio.notificar(new EventoDominio(TipoEvento.ACEPTACION, UUID.randomUUID(), PROVEEDOR, Set.of(CLIENTE)));

            assertEquals(List.of(new EmailExternoMock.CorreoEnviado(CLIENTE,
                            "Tu solicitud fue aceptada\n\nConfirma el pago con SkillCoins para reservar la sesion.")),
                    email.getEnviados());
            assertEquals(List.of(new PushExternoMock.PushEmitido(CLIENTE, "Tu solicitud fue aceptada")),
                    push.getEmitidos());
        }

        @Test
        void quienNoTienePerfilRecibeLaNotificacionEnLaBandejaInterna() {
            servicio.notificar(new EventoDominio(TipoEvento.SOLICITUD_RECIBIDA, UUID.randomUUID(), CLIENTE,
                    Set.of("usuario-sin-perfil")));

            assertEquals(1, bandeja.de("usuario-sin-perfil").size());
            assertTrue(email.getEnviados().isEmpty());
        }

        @Test
        void unCanalQueFallaNoFrenaALosDemas() {
            nuevoPerfil(CLIENTE, CanalNotificacion.EMAIL, CanalNotificacion.INTERNA);
            nuevoPerfil(PROVEEDOR, CanalNotificacion.EMAIL, CanalNotificacion.INTERNA);
            NotificacionService conEmailCaido = new NotificacionService(bandeja, perfiles,
                    List.of(new CanalQueFalla(CanalNotificacion.EMAIL), new CanalInterno(bandeja)));

            conEmailCaido.notificar(eventoEntreLasPartes(TipoEvento.CANCELACION, CLIENTE));

            assertEquals(1, bandeja.de(CLIENTE).size());
            assertEquals(1, bandeja.de(PROVEEDOR).size());
        }

        /* Aunque los canales se registren en otro orden, primero se guarda y despues se avisa afuera. */
        @Test
        void primeroSeGuardaEnLaBandejaYDespuesSeEnviaPorLosCanalesExternos() {
            nuevoPerfil(CLIENTE, CanalNotificacion.EMAIL, CanalNotificacion.PUSH);
            List<CanalNotificacion> orden = new ArrayList<>();
            NotificacionService registrando = new NotificacionService(bandeja, perfiles, List.of(
                    new CanalQueRegistra(CanalNotificacion.EMAIL, orden),
                    new CanalQueRegistra(CanalNotificacion.PUSH, orden),
                    new CanalQueRegistra(CanalNotificacion.INTERNA, orden)));

            registrando.notificar(new EventoDominio(TipoEvento.ACEPTACION, UUID.randomUUID(), PROVEEDOR,
                    Set.of(CLIENTE)));

            assertEquals(List.of(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL, CanalNotificacion.PUSH), orden);
        }

        /* Si no se pudo registrar la notificacion, no sale un email de algo que no figura en la bandeja. */
        @Test
        void siNoSePuedeGuardarEnLaBandejaNoSeEnviaNadaAfuera() {
            nuevoPerfil(CLIENTE, CanalNotificacion.EMAIL);
            nuevoPerfil(PROVEEDOR, CanalNotificacion.EMAIL);
            NotificacionService conBandejaCaida = new NotificacionService(bandeja, perfiles,
                    List.of(new CanalQueFalla(CanalNotificacion.INTERNA), new EmailAdapter(email)));

            conBandejaCaida.notificar(eventoEntreLasPartes(TipoEvento.CANCELACION, CLIENTE));

            assertTrue(email.getEnviados().isEmpty());
        }

        @Test
        void configurarCanalesReemplazaYGuardaLaPreferencia() {
            nuevoPerfil(CLIENTE, CanalNotificacion.INTERNA);
            int guardadosAntes = perfiles.guardados();

            servicio.configurarCanales(CLIENTE, Set.of(CanalNotificacion.PUSH));

            PreferenciaNotificacion guardada = perfiles.buscarPorUserId(CLIENTE).orElseThrow()
                    .getPreferenciaNotificacion();
            assertTrue(guardada.permite(CanalNotificacion.PUSH));
            assertFalse(guardada.permite(CanalNotificacion.INTERNA));
            assertEquals(guardadosAntes + 1, perfiles.guardados());
            assertThrows(IllegalArgumentException.class, () -> servicio.configurarCanales(CLIENTE, Set.of()));
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.configurarCanales("inexistente", Set.of(CanalNotificacion.EMAIL)));
        }
    }

    @Nested
    @DisplayName("Bandeja de notificaciones")
    class Bandeja {

        @Test
        void cadaUsuarioSoloVeSusNotificacionesComoDTO() {
            Notificacion propia = new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "Propia", "d");
            bandeja.guardar(propia);
            bandeja.guardar(new Notificacion(PROVEEDOR, TipoEvento.ACEPTACION, "Ajena", "d"));

            List<NotificacionDTO> recibidas = servicio.listar(CLIENTE, 0, 20).getElementos();

            assertEquals(1, recibidas.size());
            NotificacionDTO dto = recibidas.get(0);
            assertEquals(propia.getId(), dto.getId());
            assertEquals("Propia", dto.getTitulo());
            assertEquals(LocalDateTime.ofInstant(propia.getFecha(), ZoneId.systemDefault()), dto.getFecha());
            assertEquals("ACEPTACION", dto.getTipo());
            assertFalse(dto.isLeida());
        }

        @Test
        void unUsuarioSoloPuedeMarcarComoLeidasSusNotificaciones() {
            Notificacion propia = new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "Propia", "d");
            Notificacion ajena = new Notificacion(PROVEEDOR, TipoEvento.ACEPTACION, "Ajena", "d");
            bandeja.guardar(propia);
            bandeja.guardar(ajena);

            servicio.marcarLeida(CLIENTE, propia.getId());

            assertTrue(propia.isLeida());
            assertThrows(SecurityException.class, () -> servicio.marcarLeida(CLIENTE, ajena.getId()));
            assertFalse(ajena.isLeida());
            assertThrows(IllegalArgumentException.class, () -> servicio.marcarLeida(CLIENTE, UUID.randomUUID()));
        }

        @Test
        void marcarTodasLeidasSoloAfectaAlUsuario() {
            bandeja.guardar(new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "a", "d"));
            bandeja.guardar(new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "b", "d"));
            Notificacion ajena = new Notificacion(PROVEEDOR, TipoEvento.ACEPTACION, "c", "d");
            bandeja.guardar(ajena);

            servicio.marcarTodasLeidas(CLIENTE);

            assertTrue(servicio.listar(CLIENTE, 0, 20).getElementos().stream().allMatch(NotificacionDTO::isLeida));
            assertFalse(ajena.isLeida());
        }

        @Test
        void laBandejaSePagina() {
            for (int i = 0; i < 3; i++) {
                bandeja.guardar(new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "n" + i, "d"));
            }

            Pagina<NotificacionDTO> primera = servicio.listar(CLIENTE, 0, 2);
            Pagina<NotificacionDTO> segunda = servicio.listar(CLIENTE, 1, 2);

            assertEquals(2, primera.getElementos().size());
            assertEquals(1, segunda.getElementos().size());
            assertEquals(3, segunda.getTotalElementos());
            assertThrows(IllegalArgumentException.class, () -> servicio.listar(CLIENTE, -1, 2));
        }

        @Test
        void elServicioValidaSusDatos() {
            assertThrows(IllegalArgumentException.class, () -> servicio.notificar(null));
            assertThrows(IllegalArgumentException.class, () -> servicio.listar(" ", 0, 10));
            assertThrows(IllegalArgumentException.class, () -> servicio.marcarTodasLeidas(null));
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.configurarCanales("", Set.of(CanalNotificacion.EMAIL)));
            assertThrows(IllegalArgumentException.class, () -> new NotificacionService(bandeja, perfiles,
                    List.of(new EmailAdapter(email), new EmailAdapter(new EmailExternoMock()))));
            assertThrows(IllegalArgumentException.class,
                    () -> new NotificacionService(bandeja, perfiles, List.of()));
        }

        /* Sin userId el error es "falta el usuario", no "la notificacion es de otro usuario". */
        @Test
        void marcarLeidaSinUsuarioNoSeInformaComoNotificacionAjena() {
            Notificacion notificacion = new Notificacion(CLIENTE, TipoEvento.ACEPTACION, "Titulo", "d");
            bandeja.guardar(notificacion);

            IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                    () -> servicio.marcarLeida(null, notificacion.getId()));
            assertEquals("El userId es obligatorio", error.getMessage());
            assertFalse(notificacion.isLeida());
        }

        /* Sin canal interno las notificaciones no quedarian guardadas en ninguna bandeja. */
        @Test
        void elServicioExigeElCanalInterno() {
            assertThrows(IllegalArgumentException.class, () -> new NotificacionService(bandeja, perfiles,
                    List.of(new EmailAdapter(email), new PushAdapter(push))));
        }
    }

    @Nested
    @DisplayName("Observer: publicador de eventos y observador de notificaciones")
    class Observer {

        private final PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
        private final EventoDominio evento =
                new EventoDominio(TipoEvento.ACEPTACION, UUID.randomUUID(), PROVEEDOR, Set.of(CLIENTE));

        @Test
        void todosLosObservadoresSuscriptosRecibenElEvento() {
            List<EventoDominio> primero = new ArrayList<>();
            List<EventoDominio> segundo = new ArrayList<>();
            publicador.suscribir(primero::add);
            publicador.suscribir(segundo::add);

            publicador.publicarDespuesDelCommit(evento);

            assertEquals(List.of(evento), primero);
            assertEquals(List.of(evento), segundo);
        }

        @Test
        void unObservadorSuscriptoDosVecesRecibeElEventoUnaSolaVez() {
            List<EventoDominio> recibidos = new ArrayList<>();
            ObservadorEvento observador = recibidos::add;
            publicador.suscribir(observador);
            publicador.suscribir(observador);

            publicador.publicarDespuesDelCommit(evento);

            assertEquals(1, recibidos.size());
        }

        /* Suscribir mientras se avisa no rompe el recorrido: el nuevo recibe recien el proximo evento. */
        @Test
        void suscribirDuranteUnAvisoNoRompeElRecorrido() {
            List<EventoDominio> delQueLlegaTarde = new ArrayList<>();
            ObservadorEvento llegaTarde = delQueLlegaTarde::add;
            publicador.suscribir(eventoRecibido -> publicador.suscribir(llegaTarde));

            publicador.publicarDespuesDelCommit(evento);
            assertTrue(delQueLlegaTarde.isEmpty());

            publicador.publicarDespuesDelCommit(evento);
            assertEquals(List.of(evento), delQueLlegaTarde);
        }

        @Test
        void unObservadorQueFallaNoImpideAvisarALosDemas() {
            List<EventoDominio> recibidos = new ArrayList<>();
            publicador.suscribir(e -> {
                throw new IllegalStateException("observador roto");
            });
            publicador.suscribir(recibidos::add);

            publicador.publicarDespuesDelCommit(evento);

            assertEquals(List.of(evento), recibidos);
        }

        @Test
        void elObservadorDeNotificacionesDelegaElEventoEnElServicio() {
            publicador.suscribir(new NotificacionesObserver(servicio));

            publicador.publicarDespuesDelCommit(evento);

            assertEquals("Tu solicitud fue aceptada", bandeja.de(CLIENTE).get(0).getTitulo());
        }

        @Test
        void elPublicadorYElObservadorRechazanDatosNulos() {
            assertThrows(IllegalArgumentException.class, () -> publicador.publicarDespuesDelCommit(null));
            assertThrows(IllegalArgumentException.class, () -> publicador.suscribir(null));
            assertThrows(IllegalArgumentException.class, () -> new NotificacionesObserver(null));
        }
    }

    @Nested
    @DisplayName("Adapter: canales de envio")
    class Adapter {

        private final Notificacion notificacion =
                new Notificacion("ana", TipoEvento.ACEPTACION, "Tu solicitud fue aceptada", "Ya podes pagar la sesion.");

        @Test
        void cadaAdapterLeEntregaAlProveedorExternoLoQueEsteEspera() {
            EmailExternoMock proveedorDeEmail = new EmailExternoMock();
            PushExternoMock proveedorDePush = new PushExternoMock();
            EmailAdapter adapterDeEmail = new EmailAdapter(proveedorDeEmail);
            PushAdapter adapterDePush = new PushAdapter(proveedorDePush);

            adapterDeEmail.enviar(notificacion);
            adapterDePush.enviar(notificacion);

            assertEquals(List.of(new EmailExternoMock.CorreoEnviado("ana",
                    "Tu solicitud fue aceptada\n\nYa podes pagar la sesion.")), proveedorDeEmail.getEnviados());
            assertEquals(List.of(new PushExternoMock.PushEmitido("ana", "Tu solicitud fue aceptada")),
                    proveedorDePush.getEmitidos());
            assertEquals(CanalNotificacion.EMAIL, adapterDeEmail.tipo());
            assertEquals(CanalNotificacion.PUSH, adapterDePush.tipo());
        }

        @Test
        void elCanalInternoGuardaLaNotificacionEnLaBandeja() {
            CanalInterno canal = new CanalInterno(bandeja);

            canal.enviar(notificacion);

            assertEquals(1, bandeja.de("ana").size());
            assertSame(notificacion, bandeja.de("ana").get(0));
            assertEquals(CanalNotificacion.INTERNA, canal.tipo());
        }

        @Test
        void losProveedoresExternosNoSePuedenModificarDesdeAfuera() {
            EmailExternoMock proveedor = new EmailExternoMock();
            proveedor.enviarCorreo("ana", "hola");

            assertThrows(UnsupportedOperationException.class, () -> proveedor.getEnviados().clear());
            assertEquals(1, proveedor.getEnviados().size());
        }

        @Test
        void losCanalesRechazanDatosFaltantes() {
            assertThrows(IllegalArgumentException.class, () -> new EmailAdapter(null));
            assertThrows(IllegalArgumentException.class, () -> new PushAdapter(null));
            assertThrows(IllegalArgumentException.class, () -> new CanalInterno(null));
            assertThrows(IllegalArgumentException.class,
                    () -> new EmailAdapter(new EmailExternoMock()).enviar(null));
            assertThrows(IllegalArgumentException.class,
                    () -> new PushAdapter(new PushExternoMock()).enviar(null));
            assertThrows(IllegalArgumentException.class, () -> new CanalInterno(bandeja).enviar(null));
            assertThrows(IllegalArgumentException.class, () -> new EmailExternoMock().enviarCorreo(" ", "x"));
            assertTrue(new PushExternoMock().getEmitidos().isEmpty());
        }
    }

    /**
     * El controller solo valida la entrada, convierte los datos y delega en el servicio:
     * lo que ya esta probado en el servicio no se repite aca.
     */
    @Nested
    @DisplayName("NotificacionController")
    class Controller {

        private final NotificacionController controller = new NotificacionController(servicio);

        @BeforeEach
        void registrarALasPartes() {
            perfiles.guardar(new Perfil(CLIENTE, "Clara", "", ZoneId.of("UTC")));
            perfiles.guardar(new Perfil(PROVEEDOR, "Pablo", "", ZoneId.of("UTC")));
        }

        @Test
        void listarDevuelveDTOsSoloDelUsuario() {
            assertTrue(controller.listar(CLIENTE).getElementos().isEmpty());

            servicio.notificar(eventoEntreLasPartes(TipoEvento.ACEPTACION, PROVEEDOR));

            Pagina<NotificacionDTO> bandejaDelCliente = controller.listar(CLIENTE);
            assertEquals(0, bandejaDelCliente.getNumero());
            assertEquals(20, bandejaDelCliente.getTamanio());
            assertEquals(1, bandejaDelCliente.getTotalElementos());
            List<NotificacionDTO> delCliente = bandejaDelCliente.getElementos();
            assertEquals(1, delCliente.size());
            NotificacionDTO dto = delCliente.get(0);
            assertNotNull(dto.getId());
            assertEquals("Tu solicitud fue aceptada", dto.getTitulo());
            assertFalse(dto.getDetalle().isBlank());
            assertNotNull(dto.getFecha());
            assertFalse(dto.isLeida());
            assertEquals("ACEPTACION", dto.getTipo());
            assertEquals("Aceptaste la solicitud", controller.listar(PROVEEDOR).getElementos().get(0).getTitulo());
        }

        @Test
        void marcarLeidaRecibeElIdComoTextoYSoloLaPuedeMarcarSuDuenio() {
            servicio.notificar(eventoEntreLasPartes(TipoEvento.ACEPTACION, PROVEEDOR));
            String idDelCliente = controller.listar(CLIENTE).getElementos().get(0).getId().toString();

            assertThrows(ErrorAplicacion.class, () -> controller.marcarLeida(PROVEEDOR, idDelCliente));
            assertFalse(controller.listar(CLIENTE).getElementos().get(0).isLeida());

            controller.marcarLeida(CLIENTE, idDelCliente);

            assertTrue(controller.listar(CLIENTE).getElementos().get(0).isLeida());
            assertFalse(controller.listar(PROVEEDOR).getElementos().get(0).isLeida());
        }

        @Test
        void delegaMarcarTodasConfigurarCanalesYPaginar() {
            controller.configurarCanales(CLIENTE, Set.of(CanalNotificacion.INTERNA, CanalNotificacion.EMAIL));
            for (int i = 0; i < 3; i++) {
                servicio.notificar(eventoEntreLasPartes(TipoEvento.ACEPTACION, PROVEEDOR));
            }

            controller.marcarTodasLeidas(CLIENTE);
            Pagina<NotificacionDTO> primera = controller.listar(CLIENTE, 0, 2);
            Pagina<NotificacionDTO> segunda = controller.listar(CLIENTE, 1, 2);

            assertEquals(3, email.getEnviados().size());
            assertTrue(push.getEmitidos().isEmpty());
            assertTrue(controller.listar(CLIENTE).getElementos().stream().allMatch(NotificacionDTO::isLeida));
            assertTrue(controller.listar(PROVEEDOR).getElementos().stream().noneMatch(NotificacionDTO::isLeida));
            assertEquals(2, primera.getElementos().size());
            assertEquals(1, segunda.getElementos().size());
            assertEquals(3, primera.getTotalElementos());
            assertEquals(1, segunda.getNumero());
        }

        @Test
        void validaLaEntradaAntesDeDelegar() {
            servicio.notificar(eventoEntreLasPartes(TipoEvento.ACEPTACION, PROVEEDOR));
            String id = controller.listar(CLIENTE).getElementos().get(0).getId().toString();

            assertThrows(ErrorAplicacion.class, () -> controller.listar(" "));
            assertThrows(ErrorAplicacion.class, () -> controller.listar(null));
            assertThrows(ErrorAplicacion.class, () -> controller.marcarTodasLeidas(""));
            assertThrows(ErrorAplicacion.class, () -> controller.marcarLeida(CLIENTE, "no-es-un-uuid"));
            assertThrows(ErrorAplicacion.class, () -> controller.marcarLeida(CLIENTE, null));
            assertThrows(ErrorAplicacion.class,
                    () -> controller.marcarLeida(CLIENTE, UUID.randomUUID().toString()));
            assertThrows(ErrorAplicacion.class, () -> controller.marcarLeida(" ", id));
            assertThrows(ErrorAplicacion.class, () -> controller.configurarCanales(CLIENTE, Set.of()));
            assertThrows(ErrorAplicacion.class, () -> controller.configurarCanales(CLIENTE, null));
            assertThrows(ErrorAplicacion.class,
                    () -> controller.configurarCanales(" ", Set.of(CanalNotificacion.EMAIL)));
            assertThrows(ErrorAplicacion.class,
                    () -> controller.configurarCanales("no-existe", Set.of(CanalNotificacion.EMAIL)));
            assertThrows(ErrorAplicacion.class, () -> controller.listar(CLIENTE, -1, 2));
            assertThrows(ErrorAplicacion.class, () -> controller.listar(CLIENTE, 0, 0));
        }
    }

    // ---------- helpers compartidos ----------

    private static EventoDominio eventoEntreLasPartes(TipoEvento tipo, String actor) {
        return new EventoDominio(tipo, UUID.randomUUID(), actor, Set.of(CLIENTE, PROVEEDOR));
    }

    private void nuevoPerfil(String userId, CanalNotificacion... canales) {
        Perfil perfil = new Perfil(userId, userId, "", ZoneId.of("UTC"));
        perfil.cambiarCanalesNotificacion(Set.of(canales));
        perfiles.guardar(perfil);
    }

    /** Canal que anota en que orden se lo uso. */
    private record CanalQueRegistra(CanalNotificacion tipo, List<CanalNotificacion> orden) implements CanalEnvio {
        @Override
        public void enviar(Notificacion notificacion) {
            orden.add(tipo);
        }
    }

    /** Canal que simula un proveedor externo caido. */
    private record CanalQueFalla(CanalNotificacion tipo) implements CanalEnvio {
        @Override
        public void enviar(Notificacion notificacion) {
            throw new IllegalStateException("Proveedor caido");
        }
    }
}