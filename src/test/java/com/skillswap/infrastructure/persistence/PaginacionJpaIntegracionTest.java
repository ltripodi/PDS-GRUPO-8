package com.skillswap.infrastructure.persistence;

import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.ContratacionService;
import com.skillswap.application.contratacion.RegistroHistorialDTO;
import com.skillswap.application.pagos.MovimientoDTO;
import com.skillswap.application.pagos.PagoService;
import com.skillswap.application.pagos.WalletService;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.PoliticaSinReintegro;
import com.skillswap.domain.compartido.RegistroTransicion;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.infrastructure.evento.PublicadorEventosEnMemoria;
import com.skillswap.presentation.controller.ContratacionController;
import com.skillswap.presentation.controller.WalletController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Paginacion real (en la base) de contrataciones y movimientos: cada pagina trae solo sus registros,
 * las paginas no se pisan y juntas dan el mismo orden que el listado completo.
 * No se cierra JPAUtil aca: lo cierra PersistenciaTest.
 */
@DisplayName("Paginacion de listados contra JPA")
class PaginacionJpaIntegracionTest {

    private final PerfilRepository perfiles = new JpaPerfilRepository();
    private final JpaBilleteraRepository billeteras = new JpaBilleteraRepository();
    private final JpaContratacionRepository contrataciones = new JpaContratacionRepository();
    private final JpaOrganizacionRepository organizaciones = new JpaOrganizacionRepository();
    private final UnidadTransaccional unidad = new JpaUnidadTransaccional();
    private final PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
    private final PagoService pagos = new PagoService(contrataciones, billeteras, perfiles, organizaciones, unidad,
            publicador, new JpaPagoRepository());
    private final ContratacionService servicio = new ContratacionService(contrataciones,
            new JpaPublicacionRepository(), perfiles, organizaciones, unidad, publicador, pagos,
            new PoliticaSinReintegro(), new JpaPromocionRepository());
    private final ContratacionController controller = new ContratacionController(servicio, pagos);
    private final WalletController wallet = new WalletController(new WalletService(billeteras, unidad));

    @Test
    void lasContratacionesSePaginanEnLaBaseSinRepetirNiPerderRegistros() {
        String representante = "rep-" + UUID.randomUUID();
        Organizacion organizacion = new Organizacion("Org " + representante, representante);
        Perfil profesional = nuevoPerfil();
        persistirOrganizacion(organizacion);
        for (int i = 0; i < 5; i++) {
            Publicacion desafio = desafioPublicado(organizacion);
            controller.solicitar(profesional.getUserId(), desafio.getId(), "", desafio.getFranjas().get(0).getId(),
                    "Participar");
        }

        Pagina<Contratacion> primera = contrataciones.listarPorParticipantePaginado(representante, 0, 2);
        Pagina<Contratacion> segunda = contrataciones.listarPorParticipantePaginado(representante, 1, 2);
        Pagina<Contratacion> ultima = contrataciones.listarPorParticipantePaginado(representante, 2, 2);
        Pagina<Contratacion> fueraDeRango = contrataciones.listarPorParticipantePaginado(representante, 3, 2);

        assertEquals(List.of(2, 2, 1, 0), Stream.of(primera, segunda, ultima, fueraDeRango)
                .map(p -> p.getElementos().size()).toList());
        Stream.of(primera, segunda, ultima, fueraDeRango).forEach(p -> assertEquals(5, p.getTotalElementos()));

        List<UUID> paginadas = new ArrayList<>();
        Stream.of(primera, segunda, ultima).forEach(p -> p.getElementos().forEach(c -> paginadas.add(c.getId())));
        List<UUID> completas = contrataciones.listarPorParticipante(representante).stream()
                .map(Contratacion::getId).toList();
        assertEquals(completas, paginadas);
    }

    @Test
    void elControllerDevuelveLaPaginaPedidaYRechazaPedidosInvalidos() {
        String representante = "rep-" + UUID.randomUUID();
        Organizacion organizacion = new Organizacion("Org " + representante, representante);
        Perfil profesional = nuevoPerfil();
        persistirOrganizacion(organizacion);
        for (int i = 0; i < 3; i++) {
            Publicacion desafio = desafioPublicado(organizacion);
            controller.solicitar(profesional.getUserId(), desafio.getId(), "", desafio.getFranjas().get(0).getId(),
                    "Participar");
        }

        Pagina<ContratacionDTO> porDefecto = controller.listarContrataciones(profesional.getUserId());
        Pagina<ContratacionDTO> segunda = controller.listarContrataciones(profesional.getUserId(), 1, 2);

        assertEquals(3, porDefecto.getElementos().size());
        assertEquals(0, porDefecto.getNumero());
        assertEquals(20, porDefecto.getTamanio());
        assertEquals(1, segunda.getElementos().size());
        assertEquals(3, segunda.getTotalElementos());
        assertEquals(porDefecto.getElementos().get(2).getId(), segunda.getElementos().get(0).getId());

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> controller.listarContrataciones(profesional.getUserId(), -1, 2));
        assertEquals(ErrorAplicacion.Tipo.DATOS_INVALIDOS, error.getTipo());
        assertThrows(ErrorAplicacion.class, () -> controller.listarContrataciones(profesional.getUserId(), 0, 0));

        // Tope: no se puede pedir una pagina gigante para traer toda la tabla.
        assertEquals(3, controller.listarContrataciones(profesional.getUserId(), 0, Pagina.TAMANIO_MAXIMO)
                .getElementos().size());
        ErrorAplicacion demasiado = assertThrows(ErrorAplicacion.class,
                () -> controller.listarContrataciones(profesional.getUserId(), 0, Pagina.TAMANIO_MAXIMO + 1));
        assertEquals(ErrorAplicacion.Tipo.DATOS_INVALIDOS, demasiado.getTipo());
        assertThrows(ErrorAplicacion.class, () -> wallet.listarMovimientos(profesional.getUserId(), 0, 1_000_000));
    }

    @Test
    void losMovimientosSePaginanEnLaBase() {
        String titular = "user-" + UUID.randomUUID();
        Billetera billetera = new Billetera(titular);
        for (int monto = 1; monto <= 5; monto++) {
            billetera.acreditar(new SkillCoins(monto));
        }
        billeteras.guardar(billetera);

        Pagina<Movimiento> primera = billeteras.listarMovimientosPaginado(titular, 0, 2);
        Pagina<Movimiento> ultima = billeteras.listarMovimientosPaginado(titular, 2, 2);
        Pagina<Movimiento> fueraDeRango = billeteras.listarMovimientosPaginado(titular, 9, 2);

        assertEquals(2, primera.getElementos().size());
        assertEquals(1, ultima.getElementos().size());
        assertTrue(fueraDeRango.getElementos().isEmpty());
        assertEquals(5, fueraDeRango.getTotalElementos());

        List<UUID> paginados = new ArrayList<>();
        for (int pagina = 0; pagina < 3; pagina++) {
            billeteras.listarMovimientosPaginado(titular, pagina, 2).getElementos()
                    .forEach(m -> paginados.add(m.getId()));
        }
        assertEquals(billeteras.listarMovimientos(titular).stream().map(Movimiento::getId).toList(), paginados);

        Pagina<MovimientoDTO> delController = wallet.listarMovimientos(titular, 1, 2);
        assertEquals(2, delController.getElementos().size());
        assertEquals(5, delController.getTotalElementos());
        assertTrue(billeteras.listarMovimientosPaginado("otro-" + UUID.randomUUID(), 0, 2).getElementos().isEmpty());
    }

    @Test
    void elHistorialSePaginaEnOrdenCronologicoSinPerderRegistros() {
        String representante = "rep-" + UUID.randomUUID();
        Organizacion organizacion = new Organizacion("Org " + representante, representante);
        Perfil profesional = nuevoPerfil();
        Billetera fondos = new Billetera(profesional.getUserId());
        fondos.acreditar(new SkillCoins(500));
        billeteras.guardar(fondos);
        persistirOrganizacion(organizacion);
        Publicacion desafio = desafioPublicado(organizacion, 100);
        UUID id = controller.solicitar(profesional.getUserId(), desafio.getId(), "", desafio.getFranjas().get(0).getId(),
                "Participar");
        controller.aceptar(representante, id);
        controller.pagar(profesional.getUserId(), id);

        Pagina<RegistroHistorialDTO> completo = controller.consultarHistorial(profesional.getUserId(), id);
        long total = completo.getTotalElementos();
        assertTrue(total >= 2, "aceptar y pagar registran dos transiciones");
        assertEquals("CONFIRMADA", completo.getElementos().get((int) total - 1).getDestino());

        List<String> porPaginas = new ArrayList<>();
        for (int pagina = 0; pagina < total; pagina++) {
            Pagina<RegistroHistorialDTO> unaPorPagina = controller.consultarHistorial(representante, id, pagina, 1);
            assertEquals(1, unaPorPagina.getElementos().size());
            assertEquals(total, unaPorPagina.getTotalElementos());
            porPaginas.add(unaPorPagina.getElementos().get(0).getDestino());
        }
        assertEquals(completo.getElementos().stream().map(RegistroHistorialDTO::getDestino).toList(), porPaginas);
        assertTrue(controller.consultarHistorial(representante, id, (int) total, 1).getElementos().isEmpty());

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> controller.consultarHistorial(representante, id, 0, Pagina.TAMANIO_MAXIMO + 1));
        assertEquals(ErrorAplicacion.Tipo.DATOS_INVALIDOS, error.getTipo());
    }

    /*
     * La tabla del historial no guarda posicion: el orden lo tiene que poner la base con ORDER BY fecha.
     * Se adelanta en la base la fecha del primer registro (la aceptacion) sin cambiar su lugar fisico:
     * si el mapeo no ordenara, seguiria saliendo primero.
     */
    @Test
    void elHistorialSeLeeDeLaBaseOrdenadoPorFecha() {
        String representante = "rep-" + UUID.randomUUID();
        Organizacion organizacion = new Organizacion("Org " + representante, representante);
        Perfil profesional = nuevoPerfil();
        Billetera fondos = new Billetera(profesional.getUserId());
        fondos.acreditar(new SkillCoins(500));
        billeteras.guardar(fondos);
        persistirOrganizacion(organizacion);
        Publicacion desafio = desafioPublicado(organizacion, 100);
        UUID id = controller.solicitar(profesional.getUserId(), desafio.getId(), "", desafio.getFranjas().get(0).getId(),
                "Participar");
        controller.aceptar(representante, id);
        controller.pagar(profesional.getUserId(), id);

        int actualizadas = JPAUtil.transaccion(em -> em.createNativeQuery(
                        "UPDATE contratacion_historial SET fecha = ?1 WHERE destino = 'ACEPTADA' AND actorId = ?2")
                .setParameter(1, Timestamp.from(Instant.now().plus(1, ChronoUnit.DAYS)))
                .setParameter(2, organizacion.getId().toString())
                .executeUpdate());
        assertEquals(1, actualizadas);

        List<RegistroTransicion> leido = contrataciones.buscarPorId(id).orElseThrow().getHistorial();
        assertEquals("ACEPTADA", leido.get(leido.size() - 1).getDestino(), "la de fecha mas tardia va al final");
        assertEquals(leido.stream().sorted(Comparator.comparing(RegistroTransicion::getFecha)).toList(), leido);
        assertEquals("ACEPTADA", controller.consultarHistorial(representante, id).getElementos()
                .get(leido.size() - 1).getDestino());
    }

    /*
     * Los repositorios son puertos publicos: si alguien los llama sin pasar por el servicio, el pedido invalido
     * se rechaza antes de consultar la base (el constructor de Pagina validaria recien despues de la consulta).
     */
    @Test
    void losRepositoriosRechazanPedidosInvalidosAntesDeConsultar() {
        JpaPublicacionRepository publicaciones = new JpaPublicacionRepository();
        JpaNotificacionRepository notificaciones = new JpaNotificacionRepository();
        FiltrosBusqueda sinFiltros = new FiltrosBusqueda(null, null, null, null, null, null, null, null);
        List<Consumer<int[]>> listados = List.of(
                p -> contrataciones.listarPorParticipantePaginado("usuario", p[0], p[1]),
                p -> billeteras.listarMovimientosPaginado("usuario", p[0], p[1]),
                p -> notificaciones.listar("usuario", p[0], p[1]),
                p -> publicaciones.buscar(sinFiltros, p[0], p[1]),
                p -> publicaciones.listarPropiasPaginado("usuario", p[0], p[1]));

        for (Consumer<int[]> listado : listados) {
            assertThrows(IllegalArgumentException.class, () -> listado.accept(new int[]{-1, 10}));
            assertThrows(IllegalArgumentException.class, () -> listado.accept(new int[]{0, 0}));
            assertThrows(IllegalArgumentException.class,
                    () -> listado.accept(new int[]{0, Pagina.TAMANIO_MAXIMO + 1}));
            listado.accept(new int[]{0, Pagina.TAMANIO_MAXIMO});
        }
    }

    private Perfil nuevoPerfil() {
        Perfil perfil = new Perfil("user-" + UUID.randomUUID(), "Pro", "", ZoneId.of("UTC"));
        perfiles.guardar(perfil);
        return perfil;
    }

    private void persistirOrganizacion(Organizacion organizacion) {
        JPAUtil.transaccion(em -> {
            em.persist(organizacion);
            return null;
        });
    }

    private Publicacion desafioPublicado(Organizacion organizacion) {
        return desafioPublicado(organizacion, 0);
    }

    private Publicacion desafioPublicado(Organizacion organizacion, int precio) {
        Categoria categoria = new Categoria("cat-" + UUID.randomUUID());
        Publicacion desafio = Publicacion.crearDesafio(organizacion, "Desafio " + UUID.randomUUID(), "desc",
                categoria, Modalidad.REMOTA, new SkillCoins(precio), 120, 3);
        Instant inicio = Instant.now().plus(5, ChronoUnit.DAYS);
        desafio.agregarFranja(new FranjaHoraria(inicio, inicio.plus(2, ChronoUnit.HOURS), 3));
        desafio.publicar(organizacion.getUserIdAutorizado());
        JPAUtil.transaccion(em -> {
            em.persist(categoria);
            em.persist(desafio);
            return null;
        });
        return desafio;
    }
}
