package com.skillswap.infrastructure;
import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.calificacion.CalificacionRepository;
import com.skillswap.application.compartido.ConflictoConcurrenciaException;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.contratacion.SolicitudRepository;
import com.skillswap.application.notificacion.NotificacionesObserver;
import com.skillswap.application.notificacion.NotificacionRepository;
import com.skillswap.application.notificacion.NotificacionService;
import com.skillswap.application.pagos.BilleteraRepository;
import com.skillswap.application.pagos.PagoRepository;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.DatosPublicacionDTO;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.application.publicacion.PublicacionService;
import com.skillswap.domain.busqueda.OrdenPublicacion;
import com.skillswap.domain.calificacion.Calificacion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.Solicitud;
import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.TipoEvento;
import com.skillswap.domain.notificacion.CanalNotificacion;
import com.skillswap.domain.notificacion.Notificacion;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;
import com.skillswap.domain.pagos.Pago;
import com.skillswap.domain.perfil.DisponibilidadPerfil;
import com.skillswap.domain.perfil.Habilidad;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;
import com.skillswap.infrastructure.notificacion.CanalInterno;
import com.skillswap.infrastructure.evento.PublicadorEventosEnMemoria;
import com.skillswap.infrastructure.persistence.JpaBilleteraRepository;
import com.skillswap.infrastructure.persistence.JpaCalificacionRepository;
import com.skillswap.infrastructure.persistence.JpaContratacionRepository;
import com.skillswap.infrastructure.persistence.JpaNotificacionRepository;
import com.skillswap.infrastructure.persistence.JpaOrganizacionRepository;
import com.skillswap.infrastructure.persistence.JpaPagoRepository;
import com.skillswap.infrastructure.persistence.JpaPerfilRepository;
import com.skillswap.infrastructure.persistence.JpaPublicacionRepository;
import com.skillswap.infrastructure.persistence.JpaSolicitudRepository;
import com.skillswap.infrastructure.persistence.JpaUnidadTransaccional;
import com.skillswap.infrastructure.persistence.JPAUtil;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Pruebas de persistencia sobre H2 en memoria (el pom pisa la conexion de MySQL con -D).
 * Cada test usa datos propios (UUIDs / userIds unicos), asi no dependen del orden.
 */
class PersistenciaTest {

    private final PerfilRepository perfiles = new JpaPerfilRepository();
    private final PublicacionRepository publicaciones = new JpaPublicacionRepository();
    private final ContratacionRepository contrataciones = new JpaContratacionRepository();
    private final BilleteraRepository billeteras = new JpaBilleteraRepository();
    private final CalificacionRepository calificaciones = new JpaCalificacionRepository();
    private final NotificacionRepository notificaciones = new JpaNotificacionRepository();
    private final SolicitudRepository solicitudes = new JpaSolicitudRepository();
    private final PagoRepository pagos = new JpaPagoRepository();
    private final UnidadTransaccional unidad = new JpaUnidadTransaccional();

    /*
     * No se cierra JPAUtil al terminar: la EntityManagerFactory es estatica y la comparten
     * todas las clases de test de la misma JVM. Cerrarla aca rompia las que corrian despues.
     */

    // ---------- helpers ----------

    private static String nuevoUserId() {
        return "user-" + UUID.randomUUID();
    }

    private static void persistir(Object entidad) {
        JPAUtil.transaccion(em -> {
            em.persist(entidad);
            return null;
        });
    }

    private Perfil nuevoPerfil() {
        Perfil perfil = new Perfil(nuevoUserId(), "Ana", "bio", ZoneId.of("America/Argentina/Buenos_Aires"));
        perfiles.guardar(perfil);
        return perfil;
    }

    private Categoria nuevaCategoria() {
        Categoria categoria = new Categoria("cat-" + UUID.randomUUID());
        persistir(categoria);
        return categoria;
    }

    private Publicacion nuevaPublicacion(Perfil autor, Categoria categoria, String titulo, Modalidad modalidad, int precio) {
        Publicacion publicacion = new Publicacion(autor.getId(), TipoPublicacion.MENTORIA, titulo, "desc " + titulo,
                categoria, modalidad, new SkillCoins(precio), 60, 3);
        Instant ahora = Instant.now();
        publicacion.agregarFranja(new FranjaHoraria(ahora.plus(1, ChronoUnit.DAYS), ahora.plus(2, ChronoUnit.DAYS), 3));
        publicaciones.guardar(publicacion);
        return publicacion;
    }

    /*
     * Publicada por el dominio antes del primer guardado. No se guarda dos veces el mismo objeto:
     * con optimistic locking (V5) el segundo merge de un objeto desconectado con version vieja falla.
     */
    private Publicacion nuevaPublicacionPublicada(Perfil autor, String titulo) {
        Publicacion publicacion = new Publicacion(autor.getId(), TipoPublicacion.MENTORIA, titulo, "desc " + titulo,
                nuevaCategoria(), Modalidad.REMOTA, new SkillCoins(40), 60, 3);
        Instant ahora = Instant.now();
        publicacion.agregarFranja(new FranjaHoraria(ahora.plus(1, ChronoUnit.DAYS), ahora.plus(2, ChronoUnit.DAYS), 3));
        publicacion.agregarFranja(new FranjaHoraria(ahora.plus(3, ChronoUnit.DAYS), ahora.plus(4, ChronoUnit.DAYS), 3));
        publicacion.publicar(autor.getUserId());
        publicaciones.guardar(publicacion);
        return publicacion;
    }

    /* Atajo de los tests de consultas: fuerza PUBLICADA por JPQL sin pasar por el dominio. */
    private static void publicar(Publicacion publicacion) {
        JPAUtil.transaccion(em -> em
                .createQuery("UPDATE Publicacion p SET p.estado = :estado WHERE p.id = :id")
                .setParameter("estado", EstadoPublicacion.PUBLICADA)
                .setParameter("id", publicacion.getId())
                .executeUpdate());
    }

    /* El dominio todavia no expone operaciones para asignar la reserva: se setea por reflexion. */
    private static void asignar(Object objeto, String campo, Object valor) {
        try {
            java.lang.reflect.Field f = objeto.getClass().getDeclaredField(campo);
            f.setAccessible(true);
            f.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    private Solicitud nuevaSolicitud(Perfil cliente, Publicacion publicacion) {
        return new Solicitud(cliente.getId(), publicacion, publicacion.getFranjas().get(0).getId(), "hola", "aprender");
    }

    // ---------- tests ----------

    @Test
    void perfilSePersisteConHabilidadesInteresesDisponibilidadYPreferencias() {
        Habilidad habilidad = new Habilidad("Java-" + UUID.randomUUID());
        persistir(habilidad);

        Perfil perfil = new Perfil(nuevoUserId(), "Ana", "bio", ZoneId.of("America/Argentina/Buenos_Aires"));
        perfil.agregarHabilidad(habilidad);
        perfil.actualizarIntereses(Set.of("musica"));
        perfil.definirDisponibilidad(List.of(new DisponibilidadPerfil(Instant.parse("2030-01-01T10:00:00Z"), Instant.parse("2030-01-01T12:00:00Z"))));
        perfiles.guardar(perfil);

        Perfil leido = perfiles.buscarPorUserId(perfil.getUserId()).orElseThrow();

        assertEquals(perfil.getId(), leido.getId());
        assertEquals(ZoneId.of("America/Argentina/Buenos_Aires"), leido.getZonaHoraria());
        assertEquals(1, leido.getHabilidades().size());
        assertTrue(leido.getHabilidades().contains(habilidad));
        assertTrue(leido.getIntereses().contains("musica"));
        assertEquals(1, leido.getDisponibilidad().size());
        assertNotNull(leido.getPreferenciaNotificacion());
        assertTrue(leido.getPreferenciaNotificacion().getCanales().contains(CanalNotificacion.INTERNA));
        assertTrue(leido.isActivo());
        assertTrue(perfiles.buscarPorUserId("no-existe").isEmpty());
    }

    @Test
    void publicacionSePersisteConFranjasEtiquetasEHistorial() {
        Perfil autor = nuevoPerfil();
        Categoria categoria = nuevaCategoria();

        Publicacion publicacion = nuevaPublicacion(autor, categoria, "Mentoria de Java", Modalidad.REMOTA, 100);

        Publicacion leida = publicaciones.buscarPorId(publicacion.getId()).orElseThrow();

        assertEquals(EstadoPublicacion.BORRADOR, leida.getEstado());
        assertEquals(100, leida.getPrecioBase().getValor());
        assertEquals(categoria.getId(), leida.getCategoria().getId());
        assertEquals(1, leida.getFranjas().size());
        assertTrue(leida.getHistorial().isEmpty(), "un borrador todavia no tiene transiciones");
        assertEquals(0, leida.getFranjas().get(0).getOcupados());

        // La transicion la registra el dominio (historial inmutable) y se persiste con la publicacion.
        leida.publicar(autor.getUserId());
        publicaciones.guardar(leida);
        Publicacion publicada = publicaciones.buscarPorId(publicacion.getId()).orElseThrow();
        assertEquals(EstadoPublicacion.PUBLICADA, publicada.getEstado());
        assertEquals(1, publicada.getHistorial().size());
        assertEquals("BORRADOR", publicada.getHistorial().get(0).getOrigen());
        assertEquals("PUBLICADA", publicada.getHistorial().get(0).getDestino());
        assertEquals(autor.getUserId(), publicada.getHistorial().get(0).getActorId());
        assertThrows(UnsupportedOperationException.class, () -> publicada.getHistorial().clear(),
                "el historial no se puede modificar desde afuera");
    }

    @Test
    void busquedaFiltraOrdenaYPagina() {
        Perfil autor = nuevoPerfil();
        Categoria categoria = nuevaCategoria();
        Habilidad habilidad = new Habilidad("Musica-" + UUID.randomUUID());
        persistir(habilidad);

        Publicacion barata = nuevaPublicacion(autor, categoria, "Guitarra basica", Modalidad.REMOTA, 10);
        Publicacion media = nuevaPublicacion(autor, categoria, "Guitarra avanzada", Modalidad.PRESENCIAL, 50);
        Publicacion cara = nuevaPublicacion(autor, categoria, "Piano", Modalidad.REMOTA, 90);
        nuevaPublicacion(autor, categoria, "Borrador sin publicar", Modalidad.REMOTA, 5);
        Publicacion barataConHabilidad = publicaciones.buscarPorId(barata.getId()).orElseThrow();
        barataConHabilidad.reemplazarHabilidades(Set.of(habilidad));
        publicaciones.guardar(barataConHabilidad);
        publicar(barata);
        publicar(media);
        publicar(cara);

        // Solo las PUBLICADAS, ordenadas por menor precio, 2 por pagina
        FiltrosBusqueda todas = new FiltrosBusqueda(null, categoria.getId(), null, null, null, null, null, OrdenPublicacion.MENOR_PRECIO);
        Pagina<Publicacion> primera = publicaciones.buscar(todas, 0, 2);
        Pagina<Publicacion> segunda = publicaciones.buscar(todas, 1, 2);

        assertEquals(3, primera.getTotalElementos());
        assertEquals(List.of(barata.getId(), media.getId()), primera.getElementos().stream().map(Publicacion::getId).toList());
        assertEquals(List.of(cara.getId()), segunda.getElementos().stream().map(Publicacion::getId).toList());

        // Texto + modalidad + rango de precio
        FiltrosBusqueda guitarraPresencial = new FiltrosBusqueda("guitarra", categoria.getId(), Modalidad.PRESENCIAL, 20, 60, null, null, null);
        Pagina<Publicacion> resultado = publicaciones.buscar(guitarraPresencial, 0, 10);
        assertEquals(1, resultado.getTotalElementos());
        assertEquals(media.getId(), resultado.getElementos().get(0).getId());

        // Mas reciente / reputacion minima (el autor tiene reputacion 0, asi que 1+ no devuelve nada)
        assertEquals(3, publicaciones.buscar(new FiltrosBusqueda(null, categoria.getId(), null, null, null, null, null, OrdenPublicacion.MAS_RECIENTE), 0, 10).getTotalElementos());
        assertEquals(0, publicaciones.buscar(new FiltrosBusqueda(null, categoria.getId(), null, null, null, null, 1.0, OrdenPublicacion.REPUTACION), 0, 10).getTotalElementos());

        // Habilidad
        Pagina<Publicacion> porHabilidad = publicaciones.buscar(new FiltrosBusqueda(null, null, null, null, null, habilidad.getId(), null, null), 0, 10);
        assertEquals(1, porHabilidad.getTotalElementos());

        // Consultas por autor y candidatas
        assertEquals(3, publicaciones.buscarActivasPorAutor(autor.getUserId()).size());
        assertEquals(4, publicaciones.listarPropiasPaginado(autor.getUserId(), 0, 10).getTotalElementos());
        assertEquals(2, publicaciones.listarPropiasPaginado(autor.getUserId(), 1, 2).getElementos().size());
        assertTrue(publicaciones.listarCategorias().stream().anyMatch(c -> c.getId().equals(categoria.getId())));
        assertTrue(publicaciones.buscarCandidatas().stream().anyMatch(p -> p.getId().equals(cara.getId())));
    }

    @Test
    void contratacionPersisteEstadoReservaPropuestasEHistorial() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        Publicacion publicacion = nuevaPublicacionPublicada(proveedor, "Clase");
        FranjaHoraria franja = publicaciones.buscarPorId(publicacion.getId()).orElseThrow().getFranjas().get(0);

        Contratacion contratacion = new Contratacion(cliente.getId(), proveedor.getId(), new SkillCoins(40),
                nuevaSolicitud(cliente, publicacion));
        contratacion.aceptar(proveedor.getId()); // crea la reserva y registra la transicion en el historial
        contratacion.proponerReprogramacion(cliente.getId(), publicacion.getFranjas().get(1));
        contrataciones.guardar(contratacion);

        Contratacion leida = contrataciones.buscarPorId(contratacion.getId()).orElseThrow();

        assertEquals("ACEPTADA", leida.getEstado());
        assertEquals("ACEPTADA", leida.getEstadoActual().nombre()); // State reconstruido por @PostLoad
        assertEquals(40, leida.getPrecio().getValor());
        assertEquals(publicacion.getId(), leida.getSolicitud().getPublicacion().getId());
        assertEquals(franja.getId(), leida.getReserva().getFranjaId());
        assertTrue(leida.getReserva().isVigente());
        assertEquals(1, leida.getPropuestas().size());
        assertEquals(leida.getId(), leida.getPropuestas().get(0).getContratacionId());
        assertEquals(1, leida.getHistorial().size());
        assertEquals("ACEPTADA", leida.getHistorial().get(0).getDestino());

        // listarPorParticipante resuelve el userId contra cliente y proveedor
        assertEquals(1, contrataciones.listarPorParticipante(cliente.getUserId()).size());
        assertEquals(1, contrataciones.listarPorParticipante(proveedor.getUserId()).size());
        assertEquals(0, contrataciones.listarPorParticipante("otro-usuario").size());
    }

    /*
     * Caso de aceptacion 15: dos aceptaciones simultaneas no pueden consumir el ultimo cupo.
     * Ambas transacciones leen la franja con ocupados = 0; el @Version de FranjaHoraria
     * hace fallar el commit de la segunda, que hace rollback completo.
     */
    @Test
    void dosAceptacionesSimultaneasNoConsumenElUltimoCupo() throws Exception {
        Perfil proveedor = nuevoPerfil();
        Publicacion publicacion = new Publicacion(proveedor.getId(), TipoPublicacion.MENTORIA, "Ultimo cupo", "desc",
                nuevaCategoria(), Modalidad.REMOTA, new SkillCoins(40), 60, 1);
        Instant ahora = Instant.now();
        FranjaHoraria franja = new FranjaHoraria(ahora.plus(1, ChronoUnit.DAYS), ahora.plus(2, ChronoUnit.DAYS), 1);
        publicacion.agregarFranja(franja);
        publicacion.publicar(proveedor.getUserId()); // solo una publicacion publicada admite aceptar
        publicaciones.guardar(publicacion);

        Perfil clienteA = nuevoPerfil();
        Perfil clienteB = nuevoPerfil();
        Contratacion primera = new Contratacion(clienteA.getId(), proveedor.getId(), new SkillCoins(40),
                new Solicitud(clienteA.getId(), publicacion, franja.getId(), "", "aprender"));
        Contratacion segunda = new Contratacion(clienteB.getId(), proveedor.getId(), new SkillCoins(40),
                new Solicitud(clienteB.getId(), publicacion, franja.getId(), "", "aprender"));
        contrataciones.guardar(primera);
        contrataciones.guardar(segunda);

        CyclicBarrier ambasLeyeron = new CyclicBarrier(2);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> a = hilos.submit(() -> aceptarEnTransaccion(primera.getId(), proveedor.getId(), ambasLeyeron));
            Future<Throwable> b = hilos.submit(() -> aceptarEnTransaccion(segunda.getId(), proveedor.getId(), ambasLeyeron));
            List<Throwable> resultados = new ArrayList<>(List.of(
                    Optional.ofNullable(a.get(30, TimeUnit.SECONDS)).orElse(SIN_ERROR),
                    Optional.ofNullable(b.get(30, TimeUnit.SECONDS)).orElse(SIN_ERROR)));

            assertEquals(1, resultados.stream().filter(r -> r == SIN_ERROR).count(), "Solo una aceptacion debe confirmarse");
            assertEquals(1, resultados.stream().filter(ConflictoConcurrenciaException.class::isInstance).count(),
                    "La otra debe fallar por optimistic locking");
        } finally {
            hilos.shutdownNow();
        }

        FranjaHoraria leida = publicaciones.buscarPorId(publicacion.getId()).orElseThrow().getFranjas().get(0);
        assertEquals(1, leida.getOcupados());
        long aceptadas = List.of(primera.getId(), segunda.getId()).stream()
                .map(id -> contrataciones.buscarPorId(id).orElseThrow().getEstado())
                .filter("ACEPTADA"::equals)
                .count();
        assertEquals(1, aceptadas, "La contratacion que fallo debe quedar PENDIENTE (rollback)");
    }

    /*
     * Aceptar y rechazar en simultaneo la misma contratacion: sin la version de Contratacion (V5) las dos
     * confirmaban y la ultima pisaba el estado (por ejemplo, RECHAZADA con el cupo ocupado por una reserva).
     */
    @Test
    void aceptarYRechazarEnSimultaneoLaMismaContratacionNoSePisan() throws Exception {
        Perfil proveedor = nuevoPerfil();
        Perfil cliente = nuevoPerfil();
        Publicacion publicacion = nuevaPublicacionPublicada(proveedor, "Carrera");
        Contratacion contratacion = new Contratacion(cliente.getId(), proveedor.getId(), new SkillCoins(40),
                nuevaSolicitud(cliente, publicacion));
        contrataciones.guardar(contratacion);

        CyclicBarrier ambasLeyeron = new CyclicBarrier(2);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        List<Throwable> resultados;
        try {
            Future<Throwable> aceptar = hilos.submit(
                    () -> aceptarEnTransaccion(contratacion.getId(), proveedor.getId(), ambasLeyeron));
            Future<Throwable> rechazar = hilos.submit(() -> {
                try {
                    unidad.ejecutar(() -> {
                        Contratacion leida = contrataciones.buscarPorId(contratacion.getId()).orElseThrow();
                        esperar(ambasLeyeron);
                        leida.rechazar(proveedor.getId());
                        contrataciones.guardar(leida);
                    });
                    return null;
                } catch (RuntimeException e) {
                    return e;
                }
            });
            resultados = new ArrayList<>(List.of(
                    Optional.ofNullable(aceptar.get(30, TimeUnit.SECONDS)).orElse(SIN_ERROR),
                    Optional.ofNullable(rechazar.get(30, TimeUnit.SECONDS)).orElse(SIN_ERROR)));
        } finally {
            hilos.shutdownNow();
        }

        assertEquals(1, resultados.stream().filter(r -> r == SIN_ERROR).count(), "Solo una debe confirmarse");
        assertEquals(1, resultados.stream().filter(ConflictoConcurrenciaException.class::isInstance).count(),
                "La otra falla por optimistic locking: " + resultados);
        Contratacion final_ = contrataciones.buscarPorId(contratacion.getId()).orElseThrow();
        int ocupados = publicaciones.buscarPorId(publicacion.getId()).orElseThrow().getFranjas().get(0).getOcupados();
        assertEquals("ACEPTADA".equals(final_.getEstado()) ? 1 : 0, ocupados,
                "El cupo ocupado coincide con el estado que quedo (" + final_.getEstado() + ")");
    }

    private static final Throwable SIN_ERROR = new Throwable("sin error");

    /* Devuelve null si confirmo, o la excepcion con la que fallo. */
    private Throwable aceptarEnTransaccion(UUID contratacionId, UUID proveedorId, CyclicBarrier ambasLeyeron) {
        try {
            unidad.ejecutar(() -> {
                Contratacion contratacion = contrataciones.buscarPorId(contratacionId).orElseThrow();
                contratacion.getSolicitud().getPublicacion().getFranjas().size(); // lee la franja (version 0)
                esperar(ambasLeyeron);
                contratacion.aceptar(proveedorId);
                contrataciones.guardar(contratacion);
            });
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private static void esperar(CyclicBarrier barrera) {
        try {
            barrera.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    void solicitudesRecientesSeCuentanPorPublicacion() {
        Perfil cliente = nuevoPerfil();
        Publicacion con = nuevaPublicacion(nuevoPerfil(), nuevaCategoria(), "Con solicitudes", Modalidad.REMOTA, 10);
        Publicacion sin = nuevaPublicacion(nuevoPerfil(), nuevaCategoria(), "Sin solicitudes", Modalidad.REMOTA, 10);

        solicitudes.guardar(nuevaSolicitud(cliente, con));
        solicitudes.guardar(nuevaSolicitud(cliente, con));

        Map<UUID, Integer> conteo = solicitudes.contarRecientesPorPublicacion(
                List.of(con.getId(), sin.getId()), Instant.now().minus(1, ChronoUnit.HOURS));
        assertEquals(2, conteo.get(con.getId()));
        assertFalse(conteo.containsKey(sin.getId()));

        assertTrue(solicitudes.contarRecientesPorPublicacion(List.of(con.getId()), Instant.now().plus(1, ChronoUnit.HOURS)).isEmpty());
        assertTrue(solicitudes.contarRecientesPorPublicacion(List.of(), Instant.now()).isEmpty());
    }

    @Test
    void billeteraPersisteMovimientos() {
        String titular = nuevoUserId();
        assertNull(billeteras.buscarPorTitular(titular));

        Billetera billetera = new Billetera(titular);
        billetera.acreditar(new SkillCoins(50));
        billeteras.guardar(billetera);

        Billetera leida = billeteras.buscarPorTitular(titular);
        assertEquals(50, leida.getSaldo().getValor());

        List<Movimiento> movimientos = billeteras.listarMovimientos(titular);
        assertEquals(1, movimientos.size());
        assertEquals(50, movimientos.get(0).getMonto().getValor());
        assertTrue(billeteras.listarMovimientos("otro").isEmpty());
    }

    @Test
    void pagoSePersisteUnoPorContratacion() {
        Perfil cliente = nuevoPerfil();
        Publicacion publicacion = nuevaPublicacion(nuevoPerfil(), nuevaCategoria(), "Clase", Modalidad.REMOTA, 40);
        Contratacion contratacion = new Contratacion(cliente.getId(), UUID.randomUUID(), new SkillCoins(40),
                nuevaSolicitud(cliente, publicacion));
        contrataciones.guardar(contratacion);

        assertTrue(pagos.buscarPorContratacion(contratacion.getId()).isEmpty());
        pagos.guardar(new Pago(contratacion.getId(), new SkillCoins(40)));

        Pago leido = pagos.buscarPorContratacion(contratacion.getId()).orElseThrow();
        assertEquals(40, leido.getTotal().getValor());
        assertEquals(0, leido.getTotalReintegrado().getValor());
    }

    @Test
    void calificacionEsUnicaPorCalificadorYSoloVerificadaSiLaContratacionEstaCompletada() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        Publicacion publicacion = nuevaPublicacion(proveedor, nuevaCategoria(), "Clase", Modalidad.REMOTA, 40);
        Contratacion contratacion = new Contratacion(cliente.getId(), proveedor.getId(), new SkillCoins(40),
                nuevaSolicitud(cliente, publicacion));
        contrataciones.guardar(contratacion);

        calificaciones.guardar(new Calificacion(contratacion.getId(), cliente.getUserId(), proveedor.getUserId(), 5));

        assertTrue(calificaciones.existe(contratacion.getId(), cliente.getUserId()));
        assertFalse(calificaciones.existe(contratacion.getId(), proveedor.getUserId()));
        assertThrows(ConflictoConcurrenciaException.class,
                () -> calificaciones.guardar(new Calificacion(contratacion.getId(), cliente.getUserId(), proveedor.getUserId(), 4)));

        // Mientras la contratacion no este COMPLETADA la calificacion no es verificada
        assertTrue(calificaciones.listarVerificadas(proveedor.getUserId()).isEmpty());

        JPAUtil.transaccion(em -> em
                .createQuery("UPDATE Contratacion c SET c.estado = 'COMPLETADA' WHERE c.id = :id")
                .setParameter("id", contratacion.getId())
                .executeUpdate());

        assertEquals(1, calificaciones.listarVerificadas(proveedor.getUserId()).size());
    }

    @Test
    void notificacionesSeListanPorDestinatario() {
        String destinatario = nuevoUserId();
        Notificacion notificacion = new Notificacion(destinatario, TipoEvento.SOLICITUD_RECIBIDA, "Titulo", "Detalle");
        notificaciones.guardar(notificacion);

        assertEquals(1, notificaciones.listar(destinatario, 0, 20).getElementos().size());
        assertTrue(notificaciones.listar("otro", 0, 20).getElementos().isEmpty());
        Notificacion leida = notificaciones.buscarPorId(notificacion.getId()).orElseThrow();
        assertFalse(leida.isLeida());
        assertEquals("Titulo", leida.getTitulo());
    }

    @Test
    void unidadTransaccionalHaceRollbackSiFalla() {
        String destinatario = nuevoUserId();

        assertThrows(IllegalStateException.class, () -> unidad.ejecutar(() -> {
            notificaciones.guardar(new Notificacion(destinatario, TipoEvento.ACEPTACION, "t", "d"));
            throw new IllegalStateException("falla a mitad de la operacion");
        }));
        assertTrue(notificaciones.listar(destinatario, 0, 20).getElementos().isEmpty());

        unidad.ejecutar(() -> {
            notificaciones.guardar(new Notificacion(destinatario, TipoEvento.ACEPTACION, "t1", "d"));
            notificaciones.guardar(new Notificacion(destinatario, TipoEvento.ACEPTACION, "t2", "d"));
        });
        assertEquals(2, notificaciones.listar(destinatario, 0, 20).getElementos().size());
    }

    // ---------- 4.6: eventos y transacciones (integracion con H2) ----------

    private PublicadorEventosEnMemoria publicadorConNotificaciones() {
        PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
        NotificacionService servicio = new NotificacionService(notificaciones, perfiles,
                List.of(new CanalInterno(notificaciones)));
        publicador.suscribir(new NotificacionesObserver(servicio));
        return publicador;
    }

    private static EventoDominio aceptacionPara(String destinatario) {
        return new EventoDominio(TipoEvento.ACEPTACION, UUID.randomUUID(), "proveedor-" + UUID.randomUUID(),
                Set.of(destinatario));
    }

    @Test
    void laNotificacionSeGuardaRecienDespuesDelCommit() {
        String destinatario = nuevoUserId();
        PublicadorEventosEnMemoria publicador = publicadorConNotificaciones();

        unidad.ejecutar(() -> {
            publicador.publicarDespuesDelCommit(aceptacionPara(destinatario));
            assertTrue(notificaciones.listar(destinatario, 0, 20).getElementos().isEmpty());
        });

        List<Notificacion> guardadas = notificaciones.listar(destinatario, 0, 20).getElementos();
        assertEquals(1, guardadas.size());
        assertEquals("Tu solicitud fue aceptada", guardadas.get(0).getTitulo());
    }

    @Test
    void noSeNotificaUnaOperacionQueHizoRollback() {
        String destinatario = nuevoUserId();
        PublicadorEventosEnMemoria publicador = publicadorConNotificaciones();

        assertThrows(IllegalStateException.class, () -> unidad.ejecutar(() -> {
            publicador.publicarDespuesDelCommit(aceptacionPara(destinatario));
            throw new IllegalStateException("falla despues de publicar");
        }));

        assertTrue(notificaciones.listar(destinatario, 0, 20).getElementos().isEmpty());
    }

    @Test
    void enUnaTransaccionAnidadaElAvisoEsperaAlCommitDeLaExterna() {
        String destinatario = nuevoUserId();
        PublicadorEventosEnMemoria publicador = publicadorConNotificaciones();

        unidad.ejecutar(() -> {
            unidad.ejecutar(() -> publicador.publicarDespuesDelCommit(aceptacionPara(destinatario)));
            assertTrue(notificaciones.listar(destinatario, 0, 20).getElementos().isEmpty());
        });

        assertEquals(1, notificaciones.listar(destinatario, 0, 20).getElementos().size());
    }

    @Test
    void unErrorAlNotificarNoDeshaceLaOperacionConfirmada() {
        String destinatario = nuevoUserId();
        PublicadorEventosEnMemoria publicador = publicadorConNotificaciones();
        publicador.suscribir(evento -> {
            throw new IllegalStateException("observador roto");
        });
        Notificacion datoDelCasoDeUso = new Notificacion(destinatario, TipoEvento.ACEPTACION, "Dato del caso de uso", "d");

        unidad.ejecutar(() -> {
            notificaciones.guardar(datoDelCasoDeUso);
            publicador.publicarDespuesDelCommit(aceptacionPara(destinatario));
        });

        assertEquals(2, notificaciones.listar(destinatario, 0, 20).getElementos().size());
    }

    @Test
    void marcarLeidaQuedaGuardadoEnLaBase() {
        String destinatario = nuevoUserId();
        Notificacion notificacion = new Notificacion(destinatario, TipoEvento.ACEPTACION, "t", "d");
        notificaciones.guardar(notificacion);
        NotificacionService servicio = new NotificacionService(notificaciones, perfiles,
                List.of(new CanalInterno(notificaciones)));

        servicio.marcarLeida(destinatario, notificacion.getId());

        assertTrue(notificaciones.buscarPorId(notificacion.getId()).orElseThrow().isLeida());
    }

    @Test
    void unDesafioDeOrganizacionSePuedeGuardarYRecuperar() {
        // Arrange: organización real, con un representante autorizado
        String representante = "representante-" + UUID.randomUUID();

        Organizacion organizacion = new Organizacion(
                "Empresa de prueba", representante
        );
        persistir(organizacion);

        Categoria categoria = nuevaCategoria();

        PublicacionService servicio = new PublicacionService(
                publicaciones,
                perfiles,
                new JpaOrganizacionRepository()
        );

        DatosPublicacionDTO datos = new DatosPublicacionDTO(
                "Desafío Java",
                "Desarrollar una aplicación",
                categoria.getId(),
                Modalidad.REMOTA,
                60,
                1,
                100,
                Set.of("Java"),
                TipoPublicacion.DESAFIO,
                organizacion.getId()
        );

        // Act: crear usando el caso de uso real
        Publicacion creada = servicio.crearBorrador(
                representante, datos
        );

        // Assert: verificar que quedó guardada
        Publicacion recuperada = publicaciones
                .buscarPorId(creada.getId())
                .orElseThrow();

        assertEquals(TipoPublicacion.DESAFIO, recuperada.getTipo());
        assertEquals(organizacion.getId(), recuperada.getAutorId());
        assertEquals(organizacion.getId(),
                recuperada.getOrganizacion().getId());
    }


    /* Opcion B: en un desafio autor_id queda vacio y el autor esta en organizacion_id. */
    @Test
    void unDesafioGuardadoDejaVaciaLaColumnaDelPerfilAutorYUsaLaDeLaOrganizacion() {
        Organizacion organizacion = new Organizacion("Org " + UUID.randomUUID(), nuevoUserId());
        persistir(organizacion);
        Publicacion desafio = Publicacion.crearDesafio(organizacion, "Hackaton", "desc", nuevaCategoria(),
                Modalidad.REMOTA, new SkillCoins(0), 60, 3);
        publicaciones.guardar(desafio);

        long filas = JPAUtil.consultar(em -> ((Number) em.createNativeQuery(
                        "SELECT COUNT(*) FROM publicaciones WHERE id = :id AND autor_id IS NULL AND organizacion_id = :org")
                .setParameter("id", desafio.getId())
                .setParameter("org", organizacion.getId())
                .getSingleResult()).longValue());

        assertEquals(1, filas);
        assertEquals(organizacion.getId(), publicaciones.buscarPorId(desafio.getId()).orElseThrow().getAutorId());
    }

    /* Opcion B: la clave foranea de autor_id hacia perfiles vuelve a existir. */
    @Test
    void unaOfertaConUnPerfilAutorInexistenteNoSePuedeGuardar() {
        Publicacion huerfana = new Publicacion(UUID.randomUUID(), TipoPublicacion.MENTORIA, "Sin autor", "desc",
                nuevaCategoria(), Modalidad.REMOTA, new SkillCoins(10), 60, 3);

        assertThrows(RuntimeException.class, () -> publicaciones.guardar(huerfana));
        assertTrue(publicaciones.buscarPorId(huerfana.getId()).isEmpty());
    }

    /* Un desafio guardado aparece en la busqueda, tambien en el orden por defecto (relevancia), y va despues de las ofertas. */
    @Test
    void unDesafioGuardadoApareceEnLaBusquedaOrdenadaPorRelevancia() {
        Categoria categoria = nuevaCategoria();
        Publicacion oferta = nuevaPublicacion(nuevoPerfil(), categoria, "Oferta", Modalidad.REMOTA, 50);
        publicar(oferta);
        Organizacion organizacion = new Organizacion("Org " + UUID.randomUUID(), nuevoUserId());
        persistir(organizacion);
        Publicacion desafio = Publicacion.crearDesafio(organizacion, "Desafio", "desc", categoria,
                Modalidad.REMOTA, new SkillCoins(70), 60, 3);
        publicaciones.guardar(desafio);
        publicar(desafio);

        Pagina<Publicacion> pagina = publicaciones.buscar(
                new FiltrosBusqueda(null, categoria.getId(), null, null, null, null, null, OrdenPublicacion.RELEVANCIA), 0, 20);
        assertEquals(List.of(oferta.getId(), desafio.getId()),
                pagina.getElementos().stream().map(Publicacion::getId).toList());
        assertEquals(2, pagina.getTotalElementos());
    }
}
