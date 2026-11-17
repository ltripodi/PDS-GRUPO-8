package com.skillswap.application.publicacion;

import com.skillswap.application.DoblesEnMemoria;
import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.EstadoPublicacion;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/* Casos de uso de publicaciones (4.2) con puertos simulados. */
class PublicacionServiceTest {

    @Test
    void servicioDePublicacionesAutorizaAlAutorYPermiteDesafiosDeOrganizacion() {
        DoblesEnMemoria.Perfiles perfiles = new DoblesEnMemoria.Perfiles();
        Perfil profesional = new Perfil("profesional", "Profesional", "", ZoneId.of("UTC"));
        Perfil ajeno = new Perfil("ajeno", "Ajeno", "", ZoneId.of("UTC"));
        perfiles.guardar(profesional);
        perfiles.guardar(ajeno);
        Categoria categoria = new Categoria("Diseño");
        InMemoryPublicacionRepository publicaciones = new InMemoryPublicacionRepository(List.of(categoria));
        Organizacion organizacion = new Organizacion("Skill Org", "representante");
        PublicacionService servicio = new PublicacionService(publicaciones, perfiles,
                new UnaOrganizacionRepository(organizacion));

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

    private static final class UnaOrganizacionRepository implements OrganizacionRepository {
        private final Organizacion organizacion;

        private UnaOrganizacionRepository(Organizacion organizacion) {
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
}
