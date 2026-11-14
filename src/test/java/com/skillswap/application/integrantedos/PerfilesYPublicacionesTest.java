package com.skillswap.application.integrantedos;

import com.skillswap.application.CalificacionRepository;
import com.skillswap.application.PerfilService;
import com.skillswap.application.PublicacionService;
import com.skillswap.application.RecomendacionService;
import com.skillswap.application.UnidadTransaccional;
import com.skillswap.domain.Calificacion;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Habilidad;
import com.skillswap.domain.Modalidad;
import com.skillswap.domain.Organizacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.RecomendacionPorHabilidades;
import com.skillswap.domain.RecomendacionPorPopularidad;
import com.skillswap.domain.RegistroTransicion;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.Solicitud;
import com.skillswap.domain.TipoPublicacion;
import com.skillswap.domain.TipoRecomendacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cambios en perfiles (4.1) y publicaciones (4.2) que hicieron falta para busqueda y recomendacion (4.3).
 * Tests unitarios sin base de datos; guardar desafios en la base se prueba en PersistenciaTest.
 */
@DisplayName("4.1 y 4.2 Baja de perfil y autor de una publicacion")
class PerfilesYPublicacionesTest {

    private final Categoria categoria = new Categoria("Programacion");

    /**
     * Al dar de baja un perfil, sus publicaciones PUBLICADAS pasan a PAUSADA: dejan de verse en busqueda
     * y recomendacion, no admiten solicitudes y un perfil inactivo no puede volver a publicarlas.
     */
    @Nested
    @DisplayName("Baja de perfil")
    class BajaDePerfil {

        private static final String ANA = "ana";
        private static final String USUARIO = "usuario";

        private final Habilidad java = new Habilidad("Java");
        private final DoblesEnMemoria.Perfiles perfiles = new DoblesEnMemoria.Perfiles();
        private final DoblesEnMemoria.Publicaciones publicaciones = new DoblesEnMemoria.Publicaciones(perfiles);
        private final TransaccionEspia transaccion = new TransaccionEspia();
        private final PerfilService perfilService = new PerfilService(perfiles, ids -> List.of(),
                new SinCalificaciones(), publicaciones, new DoblesEnMemoria.Contrataciones(perfiles), transaccion);
        private Perfil ana;

        @BeforeEach
        void registrarPerfiles() {
            ana = new Perfil(ANA, "Ana", "", ZoneId.of("UTC"));
            Perfil usuario = new Perfil(USUARIO, "Uma", "", ZoneId.of("UTC"));
            usuario.agregarHabilidad(java);
            perfiles.guardar(ana);
            perfiles.guardar(usuario);
        }

        @Test
        void pausaLasPublicacionesDelPerfilYLoRegistraEnElHistorial() {
            Publicacion primera = publicada(ana, "Java inicial");
            Publicacion segunda = publicada(ana, "Java avanzado");

            perfilService.darDeBaja(ANA);

            assertFalse(perfiles.buscarPorUserId(ANA).orElseThrow().isActivo());
            assertEquals(EstadoPublicacion.PAUSADA, primera.getEstado());
            assertEquals(EstadoPublicacion.PAUSADA, segunda.getEstado());
            RegistroTransicion ultima = primera.getHistorial().get(primera.getHistorial().size() - 1);
            assertEquals("PUBLICADA", ultima.getOrigen());
            assertEquals("PAUSADA", ultima.getDestino());
            assertEquals(ANA, ultima.getActorId());
        }

        @Test
        void noTocaBorradoresNiPublicacionesDeOtrosPerfiles() {
            Publicacion borrador = borrador(ana, "Borrador");
            publicaciones.guardar(borrador);
            Perfil otro = new Perfil("otro", "Otro", "", ZoneId.of("UTC"));
            perfiles.guardar(otro);
            Publicacion ajena = publicada(otro, "Publicacion de otro");

            perfilService.darDeBaja(ANA);

            assertEquals(EstadoPublicacion.BORRADOR, borrador.getEstado());
            assertEquals(EstadoPublicacion.PUBLICADA, ajena.getEstado());
        }

        @Test
        void lasPublicacionesPausadasDejanDeRecomendarseYNoAdmitenSolicitudes() {
            Publicacion publicacion = publicada(ana, "Java");
            UUID franjaId = publicacion.getFranjas().get(0).getId();
            Perfil cliente = perfiles.buscarPorUserId(USUARIO).orElseThrow();
            RecomendacionService recomendacion = new RecomendacionService(
                    List.of(new RecomendacionPorHabilidades(), new RecomendacionPorPopularidad()),
                    publicaciones, perfiles, new DoblesEnMemoria.Solicitudes(), Duration.ofDays(7));
            assertEquals(1, recomendacion.recomendar(USUARIO, TipoRecomendacion.HABILIDADES, 0, 10).getTotalElementos());

            perfilService.darDeBaja(ANA);

            assertTrue(recomendacion.recomendar(USUARIO, TipoRecomendacion.HABILIDADES, 0, 10).getElementos().isEmpty());
            assertThrows(IllegalStateException.class,
                    () -> Solicitud.crear(cliente, publicacion, franjaId, "Hola", "Aprender"));
        }

        @Test
        void unPerfilDadoDeBajaNoPuedeVolverAPublicarlas() {
            Publicacion publicacion = publicada(ana, "Java");
            PublicacionService publicacionService = new PublicacionService(publicaciones, perfiles,
                    new DoblesEnMemoria.Organizaciones());

            perfilService.darDeBaja(ANA);

            assertThrows(IllegalStateException.class,
                    () -> publicacionService.cambiarEstado(ANA, publicacion.getId(), EstadoPublicacion.PUBLICADA));
            assertEquals(EstadoPublicacion.PAUSADA, publicacion.getEstado());
        }

        /** Decision: la baja es del perfil, no de la organizacion que representa. */
        @Test
        void noPausaLosDesafiosDeLaOrganizacionQueRepresenta() {
            Organizacion organizacion = new Organizacion("Fundacion Sur", ANA);
            Publicacion desafio = Publicacion.crearDesafio(organizacion, "Hackaton", "desc", categoria,
                    Modalidad.REMOTA, new SkillCoins(0), 60, 3);
            desafio.agregarFranja(franja());
            desafio.publicar(ANA);
            publicaciones.guardar(desafio);

            perfilService.darDeBaja(ANA);

            assertEquals(EstadoPublicacion.PUBLICADA, desafio.getEstado());
        }

        @Test
        void seHaceEnUnaTransaccionSePuedeRepetirYValidaSusDatos() {
            publicada(ana, "Java");

            perfilService.darDeBaja(ANA);
            perfilService.darDeBaja(ANA);

            assertEquals(2, transaccion.ejecutadas);
            assertFalse(perfiles.buscarPorUserId(ANA).orElseThrow().isActivo());
            assertThrows(IllegalArgumentException.class, () -> perfilService.darDeBaja("no-existe"));
            assertThrows(IllegalArgumentException.class, () -> perfilService.darDeBaja(" "));
            assertThrows(IllegalArgumentException.class, () -> new PerfilService(perfiles, ids -> List.of(),
                    new SinCalificaciones(), publicaciones, new DoblesEnMemoria.Contrataciones(perfiles), null));
        }

        private Publicacion borrador(Perfil autor, String titulo) {
            Publicacion publicacion = Publicacion.crearOferta(autor, TipoPublicacion.MENTORIA, titulo, "desc",
                    categoria, Modalidad.REMOTA, new SkillCoins(10), 60, 3);
            publicacion.agregarFranja(franja());
            publicacion.getHabilidades().add(java);
            return publicacion;
        }

        private Publicacion publicada(Perfil autor, String titulo) {
            Publicacion publicacion = borrador(autor, titulo);
            publicacion.publicar(autor.getUserId());
            publicaciones.guardar(publicacion);
            return publicacion;
        }
    }

    /**
     * En una oferta el autor es un Perfil (columna autor_id); en un desafio es una Organizacion
     * (columna organizacion_id) y autor_id queda vacio. getAutorId() devuelve el autor en los dos casos.
     */
    @Nested
    @DisplayName("Autor de una publicacion")
    class AutorDeLaPublicacion {

        @Test
        void enUnaOfertaElAutorEsElPerfil() {
            Perfil autor = new Perfil("ana", "Ana", "", ZoneId.of("UTC"));
            UUID perfilId = UUID.randomUUID();

            Publicacion oferta = Publicacion.crearOferta(autor, TipoPublicacion.MENTORIA, "Java", "desc", categoria,
                    Modalidad.REMOTA, new SkillCoins(10), 60, 3);
            Publicacion conConstructor = new Publicacion(perfilId, TipoPublicacion.SERVICIO, "Diseno", "desc",
                    categoria, Modalidad.PRESENCIAL, new SkillCoins(20), 90, 1);

            assertEquals(autor.getId(), oferta.getAutorId());
            assertSame(autor, oferta.getPerfil());
            assertNull(oferta.getOrganizacion());
            assertEquals(autor.getId(), columnaAutorId(oferta));
            assertEquals(perfilId, conConstructor.getAutorId());
            assertNull(conConstructor.getOrganizacion());
        }

        @Test
        void enUnDesafioElAutorEsLaOrganizacionYLaColumnaDelPerfilQuedaVacia() {
            Organizacion organizacion = new Organizacion("Fundacion Sur", "representante");

            Publicacion desafio = Publicacion.crearDesafio(organizacion, "Hackaton", "desc", categoria,
                    Modalidad.REMOTA, new SkillCoins(0), 60, 3);

            assertEquals(organizacion.getId(), desafio.getAutorId());
            assertSame(organizacion, desafio.getOrganizacion());
            assertNull(desafio.getPerfil());
            assertNull(columnaAutorId(desafio));
            assertEquals(TipoPublicacion.DESAFIO, desafio.getTipo());
        }

        @Test
        void unaOfertaExigeAutorYUnDesafioSoloSeCreaConUnaOrganizacion() {
            assertThrows(IllegalArgumentException.class, () -> new Publicacion(null, TipoPublicacion.MENTORIA,
                    "Java", "desc", categoria, Modalidad.REMOTA, new SkillCoins(10), 60, 3));
            assertThrows(IllegalArgumentException.class, () -> new Publicacion(UUID.randomUUID(),
                    TipoPublicacion.DESAFIO, "Hackaton", "desc", categoria, Modalidad.REMOTA, new SkillCoins(0), 60, 3));
            assertThrows(IllegalArgumentException.class, () -> Publicacion.crearDesafio(null, "Hackaton", "desc",
                    categoria, Modalidad.REMOTA, new SkillCoins(0), 60, 3));
        }

        /** Lo que se guarda en la columna autor_id (no lo que devuelve getAutorId). */
        private UUID columnaAutorId(Publicacion publicacion) {
            try {
                java.lang.reflect.Field campo = Publicacion.class.getDeclaredField("autorId");
                campo.setAccessible(true);
                return (UUID) campo.get(publicacion);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    // ---------- helpers y dobles ----------

    private static FranjaHoraria franja() {
        Instant inicio = Instant.now().plus(2, ChronoUnit.DAYS);
        return new FranjaHoraria(inicio, inicio.plus(1, ChronoUnit.HOURS), 3);
    }

    /** Cuenta cuantas veces se abrio una transaccion. */
    private static final class TransaccionEspia implements UnidadTransaccional {
        private int ejecutadas;

        @Override
        public void ejecutar(Runnable operacion) {
            ejecutadas++;
            operacion.run();
        }
    }

    private static final class SinCalificaciones implements CalificacionRepository {
        @Override
        public void guardar(Calificacion calificacion) {
        }

        @Override
        public boolean existe(UUID contratacionId, String calificadorId) {
            return false;
        }

        @Override
        public List<Calificacion> listarVerificadas(String userId) {
            return List.of();
        }
    }
}
