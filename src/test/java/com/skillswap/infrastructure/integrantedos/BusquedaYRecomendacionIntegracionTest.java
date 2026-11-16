package com.skillswap.infrastructure.integrantedos;

import com.skillswap.application.BusquedaService;
import com.skillswap.application.CategoriaDTO;
import com.skillswap.application.FiltrosBusqueda;
import com.skillswap.application.Pagina;
import com.skillswap.application.PerfilRepository;
import com.skillswap.application.PublicacionDTO;
import com.skillswap.application.PublicacionRepository;
import com.skillswap.application.RecomendacionService;
import com.skillswap.controllers.BusquedaController;
import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Categoria;
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
import com.skillswap.domain.TipoPublicacion;
import com.skillswap.domain.TipoRecomendacion;
import com.skillswap.infrastructure.JPAUtil;
import com.skillswap.infrastructure.JpaPerfilRepository;
import com.skillswap.infrastructure.JpaPublicacionRepository;
import com.skillswap.infrastructure.JpaSolicitudRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * 4.3 de punta a punta: controller -> casos de uso -> repositorios JPA sobre H2.
 * La base es compartida con otros tests: cada busqueda se acota a una categoria propia del test.
 * No se cierra JPAUtil aca: lo cierra PersistenciaTest.
 */
@DisplayName("4.3 Busqueda y recomendacion contra JPA")
class BusquedaYRecomendacionIntegracionTest {

    private final PerfilRepository perfiles = new JpaPerfilRepository();
    private final PublicacionRepository publicaciones = new JpaPublicacionRepository();

    private final RecomendacionService recomendacion = new RecomendacionService(
            List.of(new RecomendacionPorHabilidades(), new RecomendacionPorPopularidad()),
            publicaciones, perfiles, new JpaSolicitudRepository(), Duration.ofDays(7));
    private final BusquedaController controller =
            new BusquedaController(new BusquedaService(publicaciones, perfiles));

    private final Categoria categoria = persistir(new Categoria("cat-" + UUID.randomUUID()));

    @Test
    void combinaTextoModalidadPrecioYHabilidad() {
        Perfil autor = nuevoPerfil();
        Habilidad java = nuevaHabilidad();
        Publicacion buscada = nuevaOferta(autor, "Mentoria de JAVA", Modalidad.REMOTA, 40, java);
        nuevaOferta(autor, "Mentoria de Java presencial", Modalidad.PRESENCIAL, 40, java);
        nuevaOferta(autor, "Mentoria de Java cara", Modalidad.REMOTA, 90, java);
        nuevaOferta(autor, "Mentoria de Java sin la habilidad", Modalidad.REMOTA, 40);
        nuevaOferta(autor, "Fotografia", Modalidad.REMOTA, 40, java);

        List<UUID> encontradas = buscar(new FiltrosBusqueda("java", categoria.getId(), Modalidad.REMOTA,
                20, 60, java.getId(), null, null));

        assertEquals(List.of(buscada.getId()), encontradas);
    }

    @Test
    void conPrecioMaximoCeroEncuentraSoloLasGratis() {
        Perfil autor = nuevoPerfil();
        Publicacion gratis = nuevaOferta(autor, "Gratis", Modalidad.REMOTA, 0);
        nuevaOferta(autor, "Paga", Modalidad.REMOTA, 10);

        assertEquals(List.of(gratis.getId()), buscar(filtros(null, null, 0, null, null)));
    }

    @Test
    void losComodinesDelTextoSeBuscanComoTextoComun() {
        Perfil autor = nuevoPerfil();
        Publicacion conPorcentaje = nuevaOferta(autor, "Descuento del 50%", Modalidad.REMOTA, 10);
        nuevaOferta(autor, "Descuento del 500", Modalidad.REMOTA, 10);

        assertEquals(List.of(conPorcentaje.getId()), buscar(filtros("50%", null, null, null, null)));
    }

    @Test
    void porRelevanciaPrimeroLasQueTienenElTextoEnElTitulo() {
        Perfil autor = nuevoPerfil();
        Publicacion enDescripcion = nuevaOferta(autor, "Backend", "Todo sobre java", Modalidad.REMOTA, 10);
        Publicacion enTitulo = nuevaOferta(autor, "Java desde cero", "Curso", Modalidad.REMOTA, 10);

        assertEquals(List.of(enTitulo.getId(), enDescripcion.getId()), buscar(filtros("java", null, null, null, null)));
    }

    @Test
    void porReputacionFiltraElMinimoYLosDesafiosQuedanAfuera() {
        Publicacion buena = nuevaOferta(nuevoPerfil(5), "Buena", Modalidad.REMOTA, 10);
        Publicacion justa = nuevaOferta(nuevoPerfil(4), "Justa", Modalidad.REMOTA, 10);
        nuevaOferta(nuevoPerfil(3), "Floja", Modalidad.REMOTA, 10);
        nuevoDesafio("Desafio sin reputacion");

        List<UUID> encontradas = buscar(filtros(null, null, null, 4.0, OrdenPublicacion.REPUTACION));

        assertEquals(List.of(buena.getId(), justa.getId()), encontradas);
    }

    @Test
    void lasCategoriasTraenElIdQueUsaElFiltro() {
        assertTrue(controller.listarCategorias().contains(new CategoriaDTO(categoria.getId(), categoria.getNombre())));
    }

    @Test
    void lasPaginasNoRepitenNiPierdenPublicaciones() {
        Perfil autor = nuevoPerfil();
        IntStream.range(0, 5).forEach(i -> nuevaOferta(autor, "Oferta " + i, Modalidad.REMOTA, 10));
        FiltrosBusqueda filtros = filtros(null, null, null, null, OrdenPublicacion.MENOR_PRECIO);

        List<UUID> recorridas = new ArrayList<>();
        for (int pagina = 0; pagina < 3; pagina++) {
            recorridas.addAll(ids(controller.buscar(filtros, pagina, 2)));
        }

        assertEquals(5, Set.copyOf(recorridas).size());
        assertEquals(5, recorridas.size());
    }

    /* Fetch plan: la cantidad de consultas no depende de cuantas publicaciones trae la pagina (sin N+1). */
    @Test
    void buscarYRecomendarHacenLasMismasConsultasConDosResultadosQueConDiez() {
        Habilidad comun = nuevaHabilidad();
        Perfil cliente = nuevoPerfil(comun);
        Categoria conDos = persistir(new Categoria("cat-" + UUID.randomUUID()));
        Categoria conDiez = persistir(new Categoria("cat-" + UUID.randomUUID()));
        IntStream.range(0, 2).forEach(i -> nuevaOfertaCompleta(conDos, null));
        IntStream.range(0, 10).forEach(i -> nuevaOfertaCompleta(conDiez, null));

        long buscandoDos = contarConsultas(() -> controller.buscar(filtrosDeCategoria(conDos)));
        long buscandoDiez = contarConsultas(() -> controller.buscar(filtrosDeCategoria(conDiez)));

        nuevaOfertaCompleta(categoria, comun);
        long recomendandoUna = contarConsultas(() -> recomendacion.recomendar(cliente.getUserId(),
                TipoRecomendacion.HABILIDADES, 0, 20));
        IntStream.range(0, 9).forEach(i -> nuevaOfertaCompleta(categoria, comun));
        long recomendandoDiez = contarConsultas(() -> recomendacion.recomendar(cliente.getUserId(),
                TipoRecomendacion.HABILIDADES, 0, 20));

        assertTrue(buscandoDos > 0, "Las estadisticas de Hibernate deberian contar las consultas");
        assertEquals(buscandoDos, buscandoDiez, "N+1 en la busqueda");
        assertEquals(recomendandoUna, recomendandoDiez, "N+1 en la recomendacion");
    }

    /* Carga en lote de autores: trae los que existen con sus colecciones y omite los ids que no existen. */
    @Test
    void buscarPorIdsTraeLosPerfilesExistentesConSusColecciones() {
        Perfil conHabilidad = nuevoPerfil(nuevaHabilidad());
        Perfil sinHabilidades = nuevoPerfil();
        UUID inexistente = UUID.randomUUID();

        Map<UUID, Perfil> encontrados =
                perfiles.buscarPorIds(Set.of(conHabilidad.getId(), sinHabilidades.getId(), inexistente));

        assertEquals(Set.of(conHabilidad.getId(), sinHabilidades.getId()), encontrados.keySet());
        assertEquals(1, encontrados.get(conHabilidad.getId()).getHabilidades().size());
        assertTrue(encontrados.get(sinHabilidades.getId()).getHabilidades().isEmpty());
    }

    /* El contrato es el mismo en JPA y en la version por defecto del puerto (la de los dobles de prueba). */
    @Test
    void buscarPorIdsSinIdsDevuelveUnMapaVacioEnLasDosImplementaciones() {
        PerfilRepository porDefecto = new PerfilRepository() {
            @Override
            public Optional<Perfil> buscarPorUserId(String userId) {
                return Optional.empty();
            }

            @Override
            public Optional<Perfil> buscarPorId(UUID id) {
                return Optional.empty();
            }

            @Override
            public void guardar(Perfil perfil) {
            }
        };

        for (PerfilRepository repositorio : List.of(perfiles, porDefecto)) {
            assertTrue(repositorio.buscarPorIds(null).isEmpty());
            assertTrue(repositorio.buscarPorIds(Set.of()).isEmpty());
        }
    }

    // ---------- helpers ----------

    private List<UUID> buscar(FiltrosBusqueda filtros) {
        return ids(controller.buscar(filtros, 0, 20));
    }

    /* Siempre acotado a la categoria del test. */
    private FiltrosBusqueda filtros(String texto, Integer precioMinimo, Integer precioMaximo,
                                    Double reputacionMinima, OrdenPublicacion orden) {
        return new FiltrosBusqueda(texto, categoria.getId(), null, precioMinimo, precioMaximo, null,
                reputacionMinima, orden);
    }

    private static FiltrosBusqueda filtrosDeCategoria(Categoria categoria) {
        return new FiltrosBusqueda(null, categoria.getId(), null, null, null, null, null, null);
    }

    private static List<UUID> ids(Pagina<PublicacionDTO> pagina) {
        return pagina.getElementos().stream().map(PublicacionDTO::getId).toList();
    }

    private static <T> T persistir(T entidad) {
        JPAUtil.transaccion(em -> {
            em.persist(entidad);
            return null;
        });
        return entidad;
    }

    private static Habilidad nuevaHabilidad() {
        return persistir(new Habilidad("hab-" + UUID.randomUUID()));
    }

    private Perfil nuevoPerfil(Habilidad... habilidades) {
        return guardarPerfil(new int[0], habilidades);
    }

    /* La reputacion solo se recalcula desde calificaciones: se usa una con el puntaje indicado. */
    private Perfil nuevoPerfil(int puntaje) {
        return guardarPerfil(new int[]{puntaje});
    }

    private Perfil guardarPerfil(int[] puntajes, Habilidad... habilidades) {
        Perfil perfil = new Perfil("user-" + UUID.randomUUID(), "Ana", "", ZoneId.of("UTC"));
        for (Habilidad habilidad : habilidades) {
            perfil.agregarHabilidad(habilidad);
        }
        List<Calificacion> calificaciones = new ArrayList<>();
        for (int puntaje : puntajes) {
            calificaciones.add(new Calificacion(UUID.randomUUID(), "cliente-" + UUID.randomUUID(),
                    perfil.getUserId(), puntaje));
        }
        perfil.recalcularReputacion(calificaciones);
        perfiles.guardar(perfil);
        return perfil;
    }

    private Publicacion nuevaOferta(Perfil autor, String titulo, Modalidad modalidad, int precio,
                                    Habilidad... habilidades) {
        return nuevaOferta(autor, titulo, "Descripcion", modalidad, precio, habilidades);
    }

    private Publicacion nuevaOferta(Perfil autor, String titulo, String descripcion, Modalidad modalidad,
                                    int precio, Habilidad... habilidades) {
        Publicacion oferta = Publicacion.crearOferta(autor, TipoPublicacion.MENTORIA, titulo, descripcion,
                categoria, modalidad, new SkillCoins(precio), 60, 2);
        return publicar(oferta, autor.getUserId(), habilidades);
    }

    /* Con todo lo que lee el DTO: autor, categoria, habilidades y etiquetas. */
    private void nuevaOfertaCompleta(Categoria categoriaDeLaOferta, Habilidad habilidad) {
        Perfil autor = nuevoPerfil();
        Publicacion oferta = Publicacion.crearOferta(autor, TipoPublicacion.MENTORIA, "Oferta", "Descripcion",
                categoriaDeLaOferta, Modalidad.REMOTA, new SkillCoins(50), 60, 2);
        oferta.actualizarEtiquetas(Set.of("java", "backend"));
        publicar(oferta, autor.getUserId(), habilidad != null ? habilidad : nuevaHabilidad(), nuevaHabilidad());
    }

    private void nuevoDesafio(String titulo) {
        Organizacion organizacion = persistir(new Organizacion("Org " + UUID.randomUUID(), "user-" + UUID.randomUUID()));
        Publicacion desafio = Publicacion.crearDesafio(organizacion, titulo, "Descripcion", categoria,
                Modalidad.REMOTA, new SkillCoins(70), 60, 3);
        publicar(desafio, organizacion.getUserIdAutorizado());
    }

    private Publicacion publicar(Publicacion publicacion, String actorId, Habilidad... habilidades) {
        publicacion.getHabilidades().addAll(List.of(habilidades));
        Instant inicio = Instant.now().plus(1, ChronoUnit.DAYS);
        publicacion.agregarFranja(new FranjaHoraria(inicio, inicio.plus(1, ChronoUnit.HOURS), 2));
        publicacion.publicar(actorId);
        publicaciones.guardar(publicacion);
        return publicacion;
    }

    /* Cuenta las sentencias SQL que se preparan mientras corre la accion (estadisticas de Hibernate). */
    private static long contarConsultas(Runnable accion) {
        Statistics estadisticas = estadisticas();
        estadisticas.setStatisticsEnabled(true);
        estadisticas.clear();
        try {
            accion.run();
            return estadisticas.getPrepareStatementCount();
        } finally {
            estadisticas.setStatisticsEnabled(false);
        }
    }

    private static Statistics estadisticas() {
        EntityManager em = JPAUtil.getEntityManager();
        try {
            return em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        } finally {
            em.close();
        }
    }
}