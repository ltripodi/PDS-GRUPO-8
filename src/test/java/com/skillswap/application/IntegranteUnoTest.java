package com.skillswap.application;

import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.DisponibilidadPerfil;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Habilidad;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.Organizacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.Solicitud;
import com.skillswap.domain.TipoPublicacion;
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
                Modalidad.REMOTA, 45, 1, new SkillCoins(30), Set.of("UX"), TipoPublicacion.SERVICIO, null);
        Publicacion creada = servicio.crearBorrador("profesional", oferta);
        assertThrows(SecurityException.class, () -> servicio.agregarFranja("ajeno", creada.getId(),
                Instant.parse("2030-01-01T10:00:00Z"), Instant.parse("2030-01-01T11:00:00Z")));

        servicio.agregarFranja("profesional", creada.getId(), Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T11:00:00Z"));
        servicio.cambiarEstado("profesional", creada.getId(), EstadoPublicacion.PUBLICADA);
        assertTrue(creada.admiteSolicitud());

        DatosPublicacionDTO desafio = new DatosPublicacionDTO("Desafio", "Resolver problema", categoria.getId(),
                Modalidad.HIBRIDA, 90, 3, new SkillCoins(100), Set.of(), TipoPublicacion.DESAFIO, organizacion.getId());
        Publicacion publicacionDeOrganizacion = servicio.crearBorrador("representante", desafio);
        servicio.agregarFranja("representante", publicacionDeOrganizacion.getId(),
                Instant.parse("2030-01-02T10:00:00Z"), Instant.parse("2030-01-02T11:30:00Z"));
        servicio.cambiarEstado("representante", publicacionDeOrganizacion.getId(), EstadoPublicacion.PUBLICADA);

        assertEquals(TipoPublicacion.DESAFIO, publicacionDeOrganizacion.getTipo());
        assertTrue(publicacionDeOrganizacion.admiteSolicitud());
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
                Runnable::run);

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

    private static final class ContratacionCompletada extends Contratacion {
        private ContratacionCompletada(Perfil cliente, Perfil proveedor) {
            super(cliente.getId(), proveedor.getId(), new SkillCoins(10), new Solicitud(cliente.getId(),
                    new Publicacion(proveedor.getId(), TipoPublicacion.MENTORIA, "Clase", "", new Categoria("Test"),
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
