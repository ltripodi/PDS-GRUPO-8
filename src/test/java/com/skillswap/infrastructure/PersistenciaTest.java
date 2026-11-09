package com.skillswap.infrastructure;

import com.skillswap.application.BilleteraRepository;
import com.skillswap.application.CalificacionRepository;
import com.skillswap.application.ContratacionRepository;
import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.NotificacionRepository;
import com.skillswap.application.Pagina;
import com.skillswap.application.PagoRepository;
import com.skillswap.application.PerfilRepository;
import com.skillswap.application.PublicacionRepository;
import com.skillswap.application.SolicitudRepository;
import com.skillswap.application.UnidadTransaccional;
import com.skillswap.domain.Billetera;
import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.DisponibilidadPerfil;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Habilidad;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.Movimiento;
import com.skillswap.domain.Notificacion;
import com.skillswap.domain.OrdenPublicacion;
import com.skillswap.domain.Pago;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.PropuestaReprogramacion;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.RegistroTransicion;
import com.skillswap.domain.Reserva;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.Solicitud;
import com.skillswap.domain.TipoPublicacion;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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

    @AfterAll
    static void cerrar() {
        JPAUtil.close();
    }

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
        publicacion.getFranjas().add(new FranjaHoraria(ahora.plus(1, ChronoUnit.DAYS), ahora.plus(2, ChronoUnit.DAYS), 3));
        publicacion.getHistorial().add(new RegistroTransicion(autor.getUserId(), "-", "BORRADOR", "alta"));
        publicaciones.guardar(publicacion);
        return publicacion;
    }

    /* La transicion BORRADOR -> PUBLICADA todavia no esta implementada en el dominio, se fuerza por JPQL. */
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
        perfil.getHabilidades().add(habilidad);
        perfil.getIntereses().add("musica");
        perfil.getDisponibilidad().add(new DisponibilidadPerfil(Instant.parse("2030-01-01T10:00:00Z"), Instant.parse("2030-01-01T12:00:00Z")));
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
        assertEquals(1, leida.getHistorial().size());
        assertEquals("BORRADOR", leida.getHistorial().get(0).getDestino());
        assertEquals(0, leida.getFranjas().get(0).getOcupados());
    }

    @Test
    void busquedaFiltraOrdenaYPagina() {
        Perfil autor = nuevoPerfil();
        Categoria categoria = nuevaCategoria();

        Publicacion barata = nuevaPublicacion(autor, categoria, "Guitarra basica", Modalidad.REMOTA, 10);
        Publicacion media = nuevaPublicacion(autor, categoria, "Guitarra avanzada", Modalidad.PRESENCIAL, 50);
        Publicacion cara = nuevaPublicacion(autor, categoria, "Piano", Modalidad.REMOTA, 90);
        nuevaPublicacion(autor, categoria, "Borrador sin publicar", Modalidad.REMOTA, 5);
        publicar(barata);
        publicar(media);
        publicar(cara);

        // Solo las PUBLICADAS, ordenadas por menor precio, 2 por pagina
        FiltrosBusqueda todas = new FiltrosBusqueda(null, categoria.getId(), null, 0, 0, null, 0, OrdenPublicacion.MENOR_PRECIO);
        Pagina<Publicacion> primera = publicaciones.buscar(todas, 0, 2);
        Pagina<Publicacion> segunda = publicaciones.buscar(todas, 1, 2);

        assertEquals(3, primera.getTotalElementos());
        assertEquals(List.of(barata.getId(), media.getId()), primera.getElementos().stream().map(Publicacion::getId).toList());
        assertEquals(List.of(cara.getId()), segunda.getElementos().stream().map(Publicacion::getId).toList());

        // Texto + modalidad + rango de precio
        FiltrosBusqueda guitarraPresencial = new FiltrosBusqueda("guitarra", categoria.getId(), Modalidad.PRESENCIAL, 20, 60, null, 0, null);
        Pagina<Publicacion> resultado = publicaciones.buscar(guitarraPresencial, 0, 10);
        assertEquals(1, resultado.getTotalElementos());
        assertEquals(media.getId(), resultado.getElementos().get(0).getId());

        // Mas reciente / reputacion minima (el autor tiene reputacion 0, asi que 1+ no devuelve nada)
        assertEquals(3, publicaciones.buscar(new FiltrosBusqueda(null, categoria.getId(), null, 0, 0, null, 0, OrdenPublicacion.MAS_RECIENTE), 0, 10).getTotalElementos());
        assertEquals(0, publicaciones.buscar(new FiltrosBusqueda(null, categoria.getId(), null, 0, 0, null, 1, OrdenPublicacion.REPUTACION), 0, 10).getTotalElementos());

        // Habilidad
        Habilidad habilidad = new Habilidad("Musica-" + UUID.randomUUID());
        persistir(habilidad);
        Publicacion conHabilidad = publicaciones.buscarPorId(barata.getId()).orElseThrow();
        conHabilidad.getHabilidades().add(habilidad);
        publicaciones.guardar(conHabilidad);
        Pagina<Publicacion> porHabilidad = publicaciones.buscar(new FiltrosBusqueda(null, null, null, 0, 0, habilidad.getId(), 0, null), 0, 10);
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
        Publicacion publicacion = nuevaPublicacion(proveedor, nuevaCategoria(), "Clase", Modalidad.REMOTA, 40);
        FranjaHoraria franja = publicaciones.buscarPorId(publicacion.getId()).orElseThrow().getFranjas().get(0);

        Contratacion contratacion = new Contratacion(cliente.getId(), proveedor.getId(), new SkillCoins(40),
                nuevaSolicitud(cliente, publicacion));
        asignar(contratacion, "reserva", new Reserva(franja, 1));
        contratacion.getPropuestas().add(new PropuestaReprogramacion(contratacion.getId(), UUID.randomUUID()));
        contratacion.getHistorial().add(new RegistroTransicion(cliente.getUserId(), "-", "PENDIENTE", "solicitud"));
        contrataciones.guardar(contratacion);

        Contratacion leida = contrataciones.buscarPorId(contratacion.getId()).orElseThrow();

        assertEquals("PENDIENTE", leida.getEstado());
        assertEquals("PENDIENTE", leida.getEstadoActual().nombre()); // State reconstruido por @PostLoad
        assertEquals(40, leida.getPrecio().getValor());
        assertEquals(publicacion.getId(), leida.getSolicitud().getPublicacion().getId());
        assertEquals(franja.getId(), leida.getReserva().getFranjaId());
        assertTrue(leida.getReserva().isVigente());
        assertEquals(1, leida.getPropuestas().size());
        assertEquals(leida.getId(), leida.getPropuestas().get(0).getContratacionId());
        assertEquals(1, leida.getHistorial().size());

        // listarPorParticipante resuelve el userId contra cliente y proveedor
        assertEquals(1, contrataciones.listarPorParticipante(cliente.getUserId()).size());
        assertEquals(1, contrataciones.listarPorParticipante(proveedor.getUserId()).size());
        assertEquals(0, contrataciones.listarPorParticipante("otro-usuario").size());
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
        billetera.getMovimientos().add(new Movimiento(titular, new SkillCoins(50), "ACREDITACION"));
        billeteras.guardar(billetera);

        Billetera leida = billeteras.buscarPorTitular(titular);
        assertEquals(0, leida.getSaldo().getValor());

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
        assertThrows(PersistenceException.class,
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
        Notificacion notificacion = new Notificacion(destinatario, "Titulo", "Detalle", "SOLICITUD_RECIBIDA");
        notificaciones.guardar(notificacion);

        assertEquals(1, notificaciones.listar(destinatario).size());
        assertTrue(notificaciones.listar("otro").isEmpty());
        Notificacion leida = notificaciones.buscarPorId(notificacion.getId()).orElseThrow();
        assertFalse(leida.isLeida());
        assertEquals("Titulo", leida.getTitulo());
    }

    @Test
    void unidadTransaccionalHaceRollbackSiFalla() {
        String destinatario = nuevoUserId();

        assertThrows(IllegalStateException.class, () -> unidad.ejecutar(() -> {
            notificaciones.guardar(new Notificacion(destinatario, "t", "d", "x"));
            throw new IllegalStateException("falla a mitad de la operacion");
        }));
        assertTrue(notificaciones.listar(destinatario).isEmpty());

        unidad.ejecutar(() -> {
            notificaciones.guardar(new Notificacion(destinatario, "t1", "d", "x"));
            notificaciones.guardar(new Notificacion(destinatario, "t2", "d", "x"));
        });
        assertEquals(2, notificaciones.listar(destinatario).size());
    }
}
