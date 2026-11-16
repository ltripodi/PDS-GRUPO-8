package com.skillswap.application.integrantedos;

import com.skillswap.application.BusquedaService;
import com.skillswap.application.CategoriaDTO;
import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.Pagina;
import com.skillswap.application.PerfilRepository;
import com.skillswap.application.PublicacionDTO;
import com.skillswap.application.PublicacionMapper;
import com.skillswap.application.PublicacionRepository;
import com.skillswap.application.RecomendacionService;
import com.skillswap.application.SolicitudRepository;
import com.skillswap.controllers.BusquedaController;
import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.EstrategiaRecomendacion;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Habilidad;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.OrdenPublicacion;
import com.skillswap.domain.Organizacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.RecomendacionPorHabilidades;
import com.skillswap.domain.RecomendacionPorPopularidad;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.Solicitud;
import com.skillswap.domain.TipoPublicacion;
import com.skillswap.domain.TipoRecomendacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Punto 4.3 con repositorios en memoria: estrategias, casos de uso, filtros y controller.
 * La consulta JPA (filtros, orden y cantidad de consultas) se prueba en BusquedaYRecomendacionIntegracionTest.
 */
@DisplayName("4.3 Busqueda y recomendacion")
class BusquedaYRecomendacionTest {

    private static final Duration UNA_SEMANA = Duration.ofDays(7);

    private final Categoria tecnologia = new Categoria("Tecnologia");
    private final Habilidad java = new Habilidad("Java");
    private final Habilidad sql = new Habilidad("SQL");
    private final Habilidad fotografia = new Habilidad("Fotografia");

    private final PerfilesEnMemoria perfiles = new PerfilesEnMemoria();
    private final PublicacionesEnMemoria publicaciones = new PublicacionesEnMemoria();
    private final SolicitudesEnMemoria solicitudes = new SolicitudesEnMemoria();

    @Nested
    @DisplayName("Estrategias")
    class Estrategias {

        @Test
        void porHabilidadesOrdenaPorCoincidenciasYDescartaLasQueNoComparten() {
            Perfil cliente = nuevoPerfil("cliente", java, sql);
            Perfil proveedor = nuevoPerfil("proveedor");
            Publicacion una = nuevaOferta(proveedor, "Una", 50, java);
            Publicacion ninguna = nuevaOferta(proveedor, "Ninguna", 50, fotografia);
            Publicacion dos = nuevaOferta(proveedor, "Dos", 50, java, sql);

            List<Publicacion> recomendadas = new RecomendacionPorHabilidades()
                    .recomendar(cliente, List.of(una, ninguna, dos), Map.of());

            assertEquals(List.of(dos, una), recomendadas);
        }

        @Test
        void porPopularidadOrdenaPorSolicitudesRecientesYDescartaLasQueNoTienen() {
            Perfil proveedor = nuevoPerfil("proveedor");
            Publicacion poco = nuevaOferta(proveedor, "Poco pedida", 50);
            Publicacion nada = nuevaOferta(proveedor, "Nunca pedida", 50);
            Publicacion mucho = nuevaOferta(proveedor, "Muy pedida", 50);

            List<Publicacion> recomendadas = new RecomendacionPorPopularidad().recomendar(null,
                    List.of(poco, nada, mucho), Map.of(poco.getId(), 2, mucho.getId(), 5));

            assertEquals(List.of(mucho, poco), recomendadas);
        }

        @Test
        void aIgualPuntajeVaPrimeroLaMasReciente() {
            Perfil cliente = nuevoPerfil("cliente", java);
            Perfil proveedor = nuevoPerfil("proveedor");
            Publicacion vieja = nuevaOferta(proveedor, "Vieja", 50, java);
            Publicacion nueva = nuevaOferta(proveedor, "Nueva", 50, java);
            asignar(vieja, "fechaCreacion", LocalDateTime.now().minusDays(3));

            List<Publicacion> recomendadas = new RecomendacionPorHabilidades()
                    .recomendar(cliente, List.of(vieja, nueva), Map.of());

            assertEquals(List.of(nueva, vieja), recomendadas);
        }
    }

    @Nested
    @DisplayName("RecomendacionService (contexto del Strategy)")
    class Recomendacion {

        @Test
        void noRecomiendaPublicacionesPropiasNiDeOrganizacionesQueRepresenta() {
            Perfil cliente = nuevoPerfil("cliente", java);
            Perfil proveedor = nuevoPerfil("proveedor");
            nuevaOferta(cliente, "Propia", 50, java);
            nuevoDesafio(new Organizacion("Org del cliente", "cliente"), "Desafio propio", java);
            Publicacion ajena = nuevaOferta(proveedor, "Ajena", 50, java);

            Pagina<PublicacionDTO> pagina = nuevoServicio().recomendar("cliente", TipoRecomendacion.HABILIDADES, 0, 10);

            assertEquals(List.of(ajena.getId()), ids(pagina));
        }

        @Test
        void soloConsultaLasSolicitudesSiLaEstrategiaLasRequiere() {
            Perfil proveedor = nuevoPerfil("proveedor");
            nuevoPerfil("cliente");
            Publicacion pedida = nuevaOferta(proveedor, "Pedida", 50);
            solicitudes.registrar(pedida.getId(), 3);
            RecomendacionService servicio = nuevoServicio();

            servicio.recomendar("cliente", TipoRecomendacion.HABILIDADES, 0, 10);
            assertEquals(0, solicitudes.consultas);

            Instant antes = Instant.now();
            Pagina<PublicacionDTO> populares =
                    servicio.recomendar("cliente", TipoRecomendacion.POPULARIDAD_RECIENTE, 0, 10);
            Instant despues = Instant.now();

            assertEquals(1, solicitudes.consultas);
            assertEquals(List.of(pedida.getId()), ids(populares));
            assertFalse(solicitudes.desdeConsultado.isBefore(antes.minus(UNA_SEMANA)));
            assertFalse(solicitudes.desdeConsultado.isAfter(despues.minus(UNA_SEMANA)));
        }

        /* Caso de aceptacion 7: cambiar la estrategia sin modificar el caso de uso. */
        @Test
        void usaLaEstrategiaRegistradaParaElTipoPedido() {
            Perfil proveedor = nuevoPerfil("proveedor");
            nuevoPerfil("cliente");
            Publicacion barata = nuevaOferta(proveedor, "Barata", 10);
            Publicacion cara = nuevaOferta(proveedor, "Cara", 90);
            RecomendacionService servicio = new RecomendacionService(List.of(new PorMayorPrecio()),
                    publicaciones, perfiles, solicitudes, UNA_SEMANA);

            Pagina<PublicacionDTO> pagina = servicio.recomendar("cliente", TipoRecomendacion.HABILIDADES, 0, 10);

            assertEquals(List.of(cara.getId(), barata.getId()), ids(pagina));
            assertThrows(IllegalArgumentException.class,
                    () -> servicio.recomendar("cliente", TipoRecomendacion.POPULARIDAD_RECIENTE, 0, 10));
        }

        @Test
        void paginaEnMemoriaYBuscaLosAutoresDeLaPaginaEnUnSoloPedido() {
            nuevoPerfil("cliente", java);
            for (int i = 0; i < 3; i++) {
                nuevaOferta(nuevoPerfil("proveedor-" + i), "Oferta " + i, 50, java);
            }
            RecomendacionService servicio = nuevoServicio();

            Pagina<PublicacionDTO> primera = servicio.recomendar("cliente", TipoRecomendacion.HABILIDADES, 0, 2);
            assertEquals(1, perfiles.busquedasEnLote, "Los dos autores de la pagina se buscan juntos");

            Pagina<PublicacionDTO> segunda = servicio.recomendar("cliente", TipoRecomendacion.HABILIDADES, 1, 2);
            Pagina<PublicacionDTO> fueraDeRango =
                    servicio.recomendar("cliente", TipoRecomendacion.HABILIDADES, Integer.MAX_VALUE, 2);

            assertEquals(2, primera.getElementos().size());
            assertEquals(1, segunda.getElementos().size());
            assertEquals(3, segunda.getTotalElementos());
            assertTrue(fueraDeRango.getElementos().isEmpty());
        }

        @Test
        void rechazaUnaConfiguracionInvalida() {
            List<EstrategiaRecomendacion> repetidas =
                    List.of(new RecomendacionPorHabilidades(), new RecomendacionPorHabilidades());

            assertThrows(IllegalArgumentException.class,
                    () -> new RecomendacionService(repetidas, publicaciones, perfiles, solicitudes, UNA_SEMANA));
            assertThrows(IllegalArgumentException.class,
                    () -> new RecomendacionService(List.of(), publicaciones, perfiles, solicitudes, UNA_SEMANA));
            assertThrows(IllegalArgumentException.class, () -> new RecomendacionService(
                    List.of(new RecomendacionPorHabilidades()), publicaciones, perfiles, solicitudes, Duration.ZERO));
        }
    }

    @Nested
    @DisplayName("BusquedaService y PublicacionMapper")
    class Busqueda {

        @Test
        void elDtoMuestraAlAutorDeLaOfertaYALaOrganizacionDelDesafio() {
            Perfil proveedor = nuevoPerfil("proveedor");
            proveedor.recalcularReputacion(List.of(new Calificacion(UUID.randomUUID(), "otro", "proveedor", 4)));
            Publicacion oferta = nuevaOferta(proveedor, "Oferta", 50);
            Publicacion desafio = nuevoDesafio(new Organizacion("Acme", "acme"), "Desafio");

            Map<UUID, PublicacionDTO> porId = new HashMap<>();
            nuevaBusqueda().buscar(sinFiltros(), 0, 10).getElementos()
                    .forEach(dto -> porId.put(dto.getId(), dto));

            assertEquals("Ana proveedor", porId.get(oferta.getId()).getProveedor());
            assertEquals(4.0, porId.get(oferta.getId()).getReputacion());
            assertEquals("Acme", porId.get(desafio.getId()).getProveedor());
            assertEquals(0.0, porId.get(desafio.getId()).getReputacion());
        }

        @Test
        void listaLasCategoriasConIdYNombre() {
            publicaciones.categorias.add(tecnologia);

            assertEquals(List.of(new CategoriaDTO(tecnologia.getId(), "Tecnologia")), nuevaBusqueda().listarCategorias());
        }
    }

    @Nested
    @DisplayName("FiltrosBusqueda")
    class Filtros {

        @Test
        void sinValoresNoFiltraYOrdenaPorRelevancia() {
            FiltrosBusqueda filtros = new FiltrosBusqueda("   ", null, null, null, null, null, null, null);

            assertNull(filtros.getTexto());
            assertNull(filtros.getPrecioMaximo());
            assertEquals(OrdenPublicacion.RELEVANCIA, filtros.getOrden());
        }

        @Test
        void aceptaPrecioMaximoCeroParaBuscarLasGratis() {
            FiltrosBusqueda gratis = new FiltrosBusqueda(null, null, null, null, 0, null, null, null);

            assertEquals(Integer.valueOf(0), gratis.getPrecioMaximo());
        }

        @Test
        void rechazaValoresInvalidos() {
            assertThrows(IllegalArgumentException.class,
                    () -> new FiltrosBusqueda(null, null, null, -1, null, null, null, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new FiltrosBusqueda(null, null, null, 60, 20, null, null, null));
            assertThrows(IllegalArgumentException.class,
                    () -> new FiltrosBusqueda(null, null, null, null, null, null, -1.0, null));
        }
    }

    @Nested
    @DisplayName("BusquedaController")
    class Controller {

        @Test
        void sinPaginaDevuelveLaPrimeraConElTamanioPorDefecto() {
            nuevoPerfil("cliente", java);
            nuevaOferta(nuevoPerfil("proveedor"), "Oferta", 50, java);
            BusquedaController controller = nuevoController();

            Pagina<PublicacionDTO> encontradas = controller.buscar(sinFiltros());
            Pagina<PublicacionDTO> recomendadas = controller.recomendar("cliente");

            assertEquals(0, encontradas.getNumero());
            assertEquals(20, encontradas.getTamanio());
            assertEquals(1, recomendadas.getElementos().size(), "Por defecto recomienda por habilidades");
        }

        @Test
        void validaLaEntrada() {
            BusquedaController controller = nuevoController();

            assertThrows(IllegalArgumentException.class, () -> controller.buscar(null));
            assertThrows(IllegalArgumentException.class, () -> controller.buscar(sinFiltros(), -1, 10));
            assertThrows(IllegalArgumentException.class, () -> controller.buscar(sinFiltros(), 0, 0));
            assertThrows(IllegalArgumentException.class, () -> controller.recomendar(" "));
            assertThrows(IllegalArgumentException.class, () -> controller.recomendar("cliente", null, 0, 10));
        }
    }

    // ---------- helpers ----------

    private static FiltrosBusqueda sinFiltros() {
        return new FiltrosBusqueda(null, null, null, null, null, null, null, null);
    }

    private Perfil nuevoPerfil(String userId, Habilidad... habilidades) {
        Perfil perfil = new Perfil(userId, "Ana " + userId, "", ZoneId.of("UTC"));
        for (Habilidad habilidad : habilidades) {
            perfil.agregarHabilidad(habilidad);
        }
        perfiles.guardar(perfil);
        return perfil;
    }

    private Publicacion nuevaOferta(Perfil autor, String titulo, int precio, Habilidad... habilidades) {
        Publicacion oferta = Publicacion.crearOferta(autor, TipoPublicacion.MENTORIA, titulo, "Descripcion",
                tecnologia, Modalidad.REMOTA, new SkillCoins(precio), 60, 2);
        return publicar(oferta, autor.getUserId(), habilidades);
    }

    private Publicacion nuevoDesafio(Organizacion organizacion, String titulo, Habilidad... habilidades) {
        Publicacion desafio = Publicacion.crearDesafio(organizacion, titulo, "Descripcion", tecnologia,
                Modalidad.REMOTA, new SkillCoins(100), 90, 3);
        return publicar(desafio, organizacion.getUserIdAutorizado(), habilidades);
    }

    private Publicacion publicar(Publicacion publicacion, String actorId, Habilidad... habilidades) {
        publicacion.getHabilidades().addAll(Set.of(habilidades));
        publicacion.agregarFranja(new FranjaHoraria(Instant.parse("2030-01-01T10:00:00Z"),
                Instant.parse("2030-01-01T11:00:00Z"), 2));
        publicacion.publicar(actorId);
        publicaciones.guardar(publicacion);
        return publicacion;
    }

    private RecomendacionService nuevoServicio() {
        return new RecomendacionService(List.of(new RecomendacionPorHabilidades(), new RecomendacionPorPopularidad()),
                publicaciones, perfiles, solicitudes, UNA_SEMANA);
    }

    private BusquedaService nuevaBusqueda() {
        return new BusquedaService(publicaciones, perfiles);
    }

    private BusquedaController nuevoController() {
        return new BusquedaController(nuevaBusqueda(), nuevoServicio());
    }

    private static List<UUID> ids(Pagina<PublicacionDTO> pagina) {
        return pagina.getElementos().stream().map(PublicacionDTO::getId).toList();
    }

    /* El dominio no permite fijar la fecha de creacion: se fija por reflexion para probar el desempate. */
    private static void asignar(Object objeto, String campo, Object valor) {
        try {
            java.lang.reflect.Field field = objeto.getClass().getDeclaredField(campo);
            field.setAccessible(true);
            field.set(objeto, valor);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    // ---------- dobles de prueba ----------

    /* Estrategia inventada: el servicio acepta cualquier EstrategiaRecomendacion. */
    private static final class PorMayorPrecio implements EstrategiaRecomendacion {
        @Override
        public TipoRecomendacion tipo() {
            return TipoRecomendacion.HABILIDADES;
        }

        @Override
        public List<Publicacion> recomendar(Perfil perfil, List<Publicacion> candidatas,
                                            Map<UUID, Integer> solicitudesRecientes) {
            return candidatas.stream()
                    .sorted(Comparator.comparingInt((Publicacion p) -> p.getPrecioBase().getValor()).reversed())
                    .toList();
        }
    }

    /* Cuenta las busquedas en lote: en JPA cada una es una consulta. */
    private static final class PerfilesEnMemoria implements PerfilRepository {
        private final Map<String, Perfil> porUserId = new HashMap<>();
        private int busquedasEnLote;

        @Override
        public Optional<Perfil> buscarPorUserId(String userId) {
            return Optional.ofNullable(porUserId.get(userId));
        }

        @Override
        public Optional<Perfil> buscarPorId(UUID id) {
            return porUserId.values().stream().filter(perfil -> perfil.getId().equals(id)).findFirst();
        }

        @Override
        public Map<UUID, Perfil> buscarPorIds(Set<UUID> ids) {
            busquedasEnLote++;
            return PerfilRepository.super.buscarPorIds(ids);
        }

        @Override
        public void guardar(Perfil perfil) {
            porUserId.put(perfil.getUserId(), perfil);
        }
    }

    /* Devuelve las publicadas en orden de alta, sin filtrar: el filtrado real es de la consulta JPA. */
    private static final class PublicacionesEnMemoria implements PublicacionRepository {
        private final List<Publicacion> publicaciones = new ArrayList<>();
        private final List<Categoria> categorias = new ArrayList<>();

        @Override
        public void guardar(Publicacion publicacion) {
            publicaciones.add(publicacion);
        }

        @Override
        public Optional<Publicacion> buscarPorId(UUID id) {
            return publicaciones.stream().filter(publicacion -> publicacion.getId().equals(id)).findFirst();
        }

        @Override
        public List<Publicacion> buscarCandidatas() {
            return publicaciones.stream().filter(Publicacion::admiteSolicitud).toList();
        }

        @Override
        public Pagina<Publicacion> buscar(FiltrosBusqueda filtros, int pagina, int tamanio) {
            return Pagina.de(buscarCandidatas(), pagina, tamanio);
        }

        @Override
        public List<Categoria> listarCategorias() {
            return categorias;
        }

        @Override
        public List<Publicacion> buscarActivasPorAutor(String userId) {
            return List.of();
        }

        @Override
        public List<Publicacion> listarPropias(String userId) {
            return List.of();
        }

        @Override
        public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
            return new Pagina<>(List.of(), pagina, tamanio, 0);
        }
    }

    private static final class SolicitudesEnMemoria implements SolicitudRepository {
        private final Map<UUID, Integer> conteo = new HashMap<>();
        private int consultas;
        private Instant desdeConsultado;

        private void registrar(UUID publicacionId, int cantidad) {
            conteo.put(publicacionId, cantidad);
        }

        @Override
        public void guardar(Solicitud solicitud) {
            conteo.merge(solicitud.getPublicacion().getId(), 1, Integer::sum);
        }

        @Override
        public Map<UUID, Integer> contarRecientesPorPublicacion(List<UUID> publicacionIds, Instant desde) {
            consultas++;
            desdeConsultado = desde;
            Map<UUID, Integer> resultado = new HashMap<>(conteo);
            resultado.keySet().retainAll(publicacionIds);
            return resultado;
        }
    }
}