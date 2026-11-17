package com.skillswap.application;

import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.calificacion.CalificacionRepository;
import com.skillswap.application.calificacion.CalificacionService;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.DatosPublicacionDTO;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.application.publicacion.PublicacionRepository;
import com.skillswap.application.publicacion.PublicacionService;
import com.skillswap.domain.calificacion.Calificacion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.PropuestaReprogramacion;
import com.skillswap.domain.contratacion.Solicitud;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IntegranteUnoTest {

    @Test
    void perfilAdministraDatosYBajaLogicaSinEditarLaReputacion() {
        Perfil perfil = new Perfil("ana", "Ana", "Bio", ZoneId.of("UTC"));
        Habilidad java = new Habilidad("Java");
        perfil.agregarHabilidad(java);
        perfil.actualizarIntereses(Set.of("arquitectura"));
        perfil.definirDisponibilidad(List.of(new DisponibilidadPerfil(
                Instant.parse("2030-01-01T10:00:00Z"), Instant.parse("2030-01-01T11:00:00Z"))));

        perfil.recalcularReputacion(List.of(
                new Calificacion(UUID.randomUUID(), "cliente-a", "ana", 4),
                new Calificacion(UUID.randomUUID(), "cliente-b", "ana", 5)));
        perfil.actualizarDatos("Ana P.", "Nueva bio", ZoneId.of("America/Argentina/Buenos_Aires"));

        assertEquals(4.5, perfil.obtenerReputacion());
        assertEquals("Ana P.", perfil.getNombreVisible());
        assertEquals(Set.of("arquitectura"), perfil.getIntereses());
        assertEquals(1, perfil.getDisponibilidad().size());

        perfil.darDeBaja();
        assertFalse(perfil.isActivo());
        assertThrows(IllegalStateException.class, () -> perfil.agregarHabilidad(new Habilidad("Kotlin")));
    }

    @Test
    void publicacionSoloSePublicaCompletaYRegistraSusTransiciones() {
        Perfil autora = new Perfil("proveedora", "Proveedora", "", ZoneId.of("UTC"));
        Publicacion publicacion = Publicacion.crearOferta(autora, TipoPublicacion.MENTORIA, "", "Descripcion",
                new Categoria("Tecnologia"), Modalidad.REMOTA, new SkillCoins(50), 60, 2);

        assertThrows(IllegalStateException.class, () -> publicacion.publicar("proveedora"));

        publicacion.editarDatos("Mentoria Java", "Descripcion", new SkillCoins(50));
        publicacion.agregarFranja(new FranjaHoraria(Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T11:00:00Z"), 2));
        publicacion.publicar("proveedora");
        publicacion.pausar("proveedora");
        publicacion.publicar("proveedora");

        assertTrue(publicacion.admiteSolicitud());
        assertEquals(3, publicacion.getHistorial().size());
        assertEquals("PAUSADA", publicacion.getHistorial().get(1).getDestino());
        assertThrows(IllegalArgumentException.class, () -> publicacion.cancelar("proveedora", " "));
    }

    @Test
    void servicioDePublicacionesAutorizaAlAutorYPermiteDesafiosDeOrganizacion() {
        InMemoryPerfilRepository perfiles = new InMemoryPerfilRepository();
        Perfil profesional = new Perfil("profesional", "Profesional", "", ZoneId.of("UTC"));
        Perfil ajeno = new Perfil("ajeno", "Ajeno", "", ZoneId.of("UTC"));
        perfiles.guardar(profesional);
        perfiles.guardar(ajeno);
        Categoria categoria = new Categoria("Diseño");
        InMemoryPublicacionRepository publicaciones = new InMemoryPublicacionRepository(List.of(categoria));
        Organizacion organizacion = new Organizacion("Skill Org", "representante");
        PublicacionService servicio = new PublicacionService(publicaciones, perfiles,
                new InMemoryOrganizacionRepository(organizacion));

        DatosPublicacionDTO oferta = new DatosPublicacionDTO("Revision", "Portfolio", categoria.getId(),
                Modalidad.REMOTA, 45, 1, 30, Set.of("UX"), TipoPublicacion.SERVICIO, null);
        Publicacion creada = servicio.crearBorrador("profesional", oferta);
        assertThrows(SecurityException.class, () -> servicio.agregarFranja("ajeno", creada.getId(),
                Instant.parse("2030-01-01T10:00:00Z"), Instant.parse("2030-01-01T11:00:00Z")));

        servicio.agregarFranja("profesional", creada.getId(), Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T11:00:00Z"));
        servicio.cambiarEstado("profesional", creada.getId(), EstadoPublicacion.PUBLICADA);
        assertTrue(creada.admiteSolicitud());

        DatosPublicacionDTO desafio = new DatosPublicacionDTO("Desafio", "Resolver problema", categoria.getId(),
                Modalidad.HIBRIDA, 90, 3, 100, Set.of(), TipoPublicacion.DESAFIO, organizacion.getId());
        Publicacion publicacionDeOrganizacion = servicio.crearBorrador("representante", desafio);
        servicio.agregarFranja("representante", publicacionDeOrganizacion.getId(),
                Instant.parse("2030-01-02T10:00:00Z"), Instant.parse("2030-01-02T11:30:00Z"));
        servicio.cambiarEstado("representante", publicacionDeOrganizacion.getId(), EstadoPublicacion.PUBLICADA);

        assertEquals(TipoPublicacion.DESAFIO, publicacionDeOrganizacion.getTipo());
        assertTrue(publicacionDeOrganizacion.admiteSolicitud());

        // Sin organizacionId (como desde la interfaz): se usa la organizacion que representa el usuario.
        DatosPublicacionDTO sinOrganizacion = new DatosPublicacionDTO("Otro desafio", "Resolver", categoria.getId(),
                Modalidad.REMOTA, 60, 2, 50, Set.of(), TipoPublicacion.DESAFIO, null);
        assertEquals(organizacion, servicio.crearBorrador("representante", sinOrganizacion).getOrganizacion());
        assertThrows(SecurityException.class, () -> servicio.crearBorrador("profesional", sinOrganizacion),
                "un perfil que no representa a una organizacion no publica desafios");
        assertThrows(IllegalArgumentException.class, () -> servicio.crearBorrador("profesional",
                new DatosPublicacionDTO("Gratis", "x", categoria.getId(), Modalidad.REMOTA, 30, 1, -5, Set.of(),
                        TipoPublicacion.MENTORIA, null)), "el precio no puede ser negativo");
    }

    @Test
    void calificacionVerificaParticipacionUnicidadYRecalculaReputacion() {
        InMemoryPerfilRepository perfiles = new InMemoryPerfilRepository();
        Perfil cliente = new Perfil("cliente", "Cliente", "", ZoneId.of("UTC"));
        Perfil proveedor = new Perfil("proveedor", "Proveedor", "", ZoneId.of("UTC"));
        Perfil externo = new Perfil("externo", "Externo", "", ZoneId.of("UTC"));
        perfiles.guardar(cliente);
        perfiles.guardar(proveedor);
        perfiles.guardar(externo);

        Contratacion contratacion = new ContratacionCompletada(cliente, proveedor);
        InMemoryContratacionRepository contrataciones = new InMemoryContratacionRepository(contratacion);
        InMemoryCalificacionRepository calificaciones = new InMemoryCalificacionRepository();
        CalificacionService servicio = new CalificacionService(calificaciones, contrataciones, perfiles,
                new InMemoryOrganizacionRepository(new Organizacion("Otra", "otro")), Runnable::run);

        servicio.calificar("cliente", contratacion.getId(), "proveedor", 5);

        assertEquals(1, calificaciones.calificaciones.size());
        assertEquals(5.0, proveedor.obtenerReputacion());
        assertThrows(IllegalStateException.class,
                () -> servicio.calificar("cliente", contratacion.getId(), "proveedor", 4));
        assertThrows(SecurityException.class,
                () -> servicio.calificar("externo", contratacion.getId(), "proveedor", 5));
        assertThrows(IllegalArgumentException.class,
                () -> new Calificacion(contratacion.getId(), "cliente", "proveedor", 6));
    }

    @Test
    void lasDosPartesDeUnDesafioPuedenCalificarseYSoloElPerfilRecalculaReputacion() {
        InMemoryPerfilRepository perfiles = new InMemoryPerfilRepository();
        Perfil cliente = new Perfil("cliente", "Cliente", "", ZoneId.of("UTC"));
        perfiles.guardar(cliente);
        Organizacion organizacion = new Organizacion("UADE", "uade-admin");
        Contratacion contratacion = new ContratacionCompletada(cliente, organizacion.getId());
        InMemoryCalificacionRepository calificaciones = new InMemoryCalificacionRepository();
        CalificacionService servicio = new CalificacionService(calificaciones,
                new InMemoryContratacionRepository(contratacion), perfiles,
                new InMemoryOrganizacionRepository(organizacion), Runnable::run);

        servicio.calificar("uade-admin", contratacion.getId(), "cliente", 4);
        servicio.calificar("cliente", contratacion.getId(), "uade-admin", 5);

        assertEquals(2, calificaciones.calificaciones.size());
        assertEquals(4.0, cliente.obtenerReputacion(),
                "la reputacion se deriva solo de calificaciones dirigidas al perfil");
        assertThrows(IllegalStateException.class,
                () -> servicio.calificar("uade-admin", contratacion.getId(), "cliente", 3));
    }

    @Test
    void losGettersDeColeccionesDelDominioNoPermitenModificarElEstadoInterno() {
        Perfil perfil = new Perfil("ana", "Ana", "", ZoneId.of("UTC"));
        Publicacion publicacion = Publicacion.crearOferta(perfil, TipoPublicacion.MENTORIA, "Clase", "",
                new Categoria("Tecnologia"), Modalidad.REMOTA, new SkillCoins(10), 60, 1);
        Billetera billetera = new Billetera("ana");
        Contratacion contratacion = new Contratacion(perfil.getId(), UUID.randomUUID(), new SkillCoins(10),
                new Solicitud(perfil.getId(), publicacion, UUID.randomUUID(), "", ""));

        assertThrows(UnsupportedOperationException.class,
                () -> perfil.getHabilidades().add(new Habilidad("Java")));
        assertThrows(UnsupportedOperationException.class, () -> perfil.getIntereses().add("backend"));
        assertThrows(UnsupportedOperationException.class,
                () -> perfil.getDisponibilidad().add(new DisponibilidadPerfil(Instant.now(), Instant.now().plusSeconds(60))));
        assertThrows(UnsupportedOperationException.class, () -> publicacion.getEtiquetas().add("java"));
        assertThrows(UnsupportedOperationException.class,
                () -> publicacion.getFranjas().add(new FranjaHoraria(Instant.now(), Instant.now().plusSeconds(60), 1)));
        assertThrows(UnsupportedOperationException.class,
                () -> publicacion.getHabilidades().add(new Habilidad("Java")));
        assertThrows(UnsupportedOperationException.class,
                () -> billetera.getMovimientos().add(new Movimiento("ana", new SkillCoins(1), "CREDITO")));
        assertThrows(UnsupportedOperationException.class,
                () -> contratacion.getPropuestas().add(new PropuestaReprogramacion(contratacion.getId(), UUID.randomUUID())));
    }

    private static final class ContratacionCompletada extends Contratacion {
        private ContratacionCompletada(Perfil cliente, Perfil proveedor) {
            this(cliente, proveedor.getId());
        }

        private ContratacionCompletada(Perfil cliente, UUID proveedorId) {
            super(cliente.getId(), proveedorId, new SkillCoins(10), new Solicitud(cliente.getId(),
                    new Publicacion(proveedorId, TipoPublicacion.MENTORIA, "Clase", "", new Categoria("Test"),
                            Modalidad.REMOTA, new SkillCoins(10), 60, 1), UUID.randomUUID(), "", ""));
        }

        @Override
        public String getEstado() {
            return "COMPLETADA";
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

    private static final class InMemoryPublicacionRepository implements PublicacionRepository {
        private final Map<UUID, Publicacion> publicaciones = new HashMap<>();
        private final List<Categoria> categorias;

        private InMemoryPublicacionRepository(List<Categoria> categorias) {
            this.categorias = categorias;
        }

        @Override
        public void guardar(Publicacion publicacion) {
            publicaciones.put(publicacion.getId(), publicacion);
        }

        @Override
        public Optional<Publicacion> buscarPorId(UUID id) {
            return Optional.ofNullable(publicaciones.get(id));
        }

        @Override
        public List<Publicacion> buscarCandidatas() {
            return publicaciones.values().stream().filter(Publicacion::admiteSolicitud).toList();
        }

        @Override
        public List<Publicacion> buscarActivasPorAutor(String userId) {
            return List.of();
        }

        @Override
        public List<Publicacion> listarPropias(String userId) {
            return new ArrayList<>(publicaciones.values());
        }

        @Override
        public Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
            return new Pagina<>(List.of(), pagina, tamanio, 0);
        }

        @Override
        public List<Categoria> listarCategorias() {
            return categorias;
        }

        @Override
        public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
            return new Pagina<>(listarPropias(userId), pagina, tamanio, publicaciones.size());
        }
    }

    private static final class InMemoryOrganizacionRepository implements OrganizacionRepository {
        private final Organizacion organizacion;

        private InMemoryOrganizacionRepository(Organizacion organizacion) {
            this.organizacion = organizacion;
        }

        @Override
        public Optional<Organizacion> buscarPorId(UUID id) {
            return organizacion.getId().equals(id) ? Optional.of(organizacion) : Optional.empty();
        }

        @Override
        public Optional<Organizacion> buscarPorUserIdAutorizado(String userId) {
            return organizacion.autoriza(userId) ? Optional.of(organizacion) : Optional.empty();
        }
    }

    private static final class InMemoryContratacionRepository implements ContratacionRepository {
        private final Contratacion contratacion;

        private InMemoryContratacionRepository(Contratacion contratacion) {
            this.contratacion = contratacion;
        }

        @Override
        public Optional<Contratacion> buscarPorId(UUID id) {
            return contratacion.getId().equals(id) ? Optional.of(contratacion) : Optional.empty();
        }

        @Override
        public List<Contratacion> listarPorParticipante(String userId) {
            return List.of();
        }

        @Override
        public void guardar(Contratacion contratacion) {
            throw new AssertionError("El caso de uso no debe modificar la contratacion al calificar");
        }
    }

    private static final class InMemoryCalificacionRepository implements CalificacionRepository {
        private final List<Calificacion> calificaciones = new ArrayList<>();

        @Override
        public void guardar(Calificacion calificacion) {
            calificaciones.add(calificacion);
        }

        @Override
        public boolean existe(UUID contratacionId, String calificadorId) {
            return calificaciones.stream().anyMatch(calificacion -> calificacion.getContratacionId().equals(contratacionId)
                    && calificacion.getCalificadorId().equals(calificadorId));
        }

        @Override
        public List<Calificacion> listarVerificadas(String userId) {
            return calificaciones.stream()
                    .filter(calificacion -> calificacion.getCalificadoId().equals(userId))
                    .toList();
        }
    }
}
