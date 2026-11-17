package com.skillswap.application.perfil;

import com.skillswap.application.DoblesEnMemoria;
import com.skillswap.application.busqueda.RecomendacionService;
import com.skillswap.application.calificacion.CalificacionRepository;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.publicacion.PublicacionService;
import com.skillswap.domain.busqueda.RecomendacionPorHabilidades;
import com.skillswap.domain.busqueda.RecomendacionPorPopularidad;
import com.skillswap.domain.busqueda.TipoRecomendacion;
import com.skillswap.domain.calificacion.Calificacion;
import com.skillswap.domain.compartido.RegistroTransicion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.Solicitud;
import com.skillswap.domain.perfil.Habilidad;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Baja de perfil (4.1). Al dar de baja un perfil, sus publicaciones PUBLICADAS pasan a PAUSADA: dejan de verse
 * en busqueda y recomendacion, no admiten solicitudes y un perfil inactivo no puede volver a publicarlas.
 */
@DisplayName("4.1 Baja de perfil")
class BajaDePerfilTest {

    private static final String ANA = "ana";
    private static final String USUARIO = "usuario";

    private final Categoria categoria = new Categoria("Programacion");
    private final Habilidad java = new Habilidad("Java");
    private final DoblesEnMemoria.Perfiles perfiles = new DoblesEnMemoria.Perfiles();
    private final DoblesEnMemoria.Publicaciones publicaciones = new DoblesEnMemoria.Publicaciones(perfiles);
    private final TransaccionEspia transaccion = new TransaccionEspia();
    private final DoblesEnMemoria.Contrataciones contrataciones = new DoblesEnMemoria.Contrataciones(perfiles);
    private final PerfilService perfilService = new PerfilService(perfiles, ids -> List.of(),
            new SinCalificaciones(), publicaciones, contrataciones, transaccion);
    private Perfil ana;

    /* Si se diera de baja, la otra parte todavia podria aceptar o pagar esas contrataciones. */
    @Test
    void noSePuedeDarDeBajaConContratacionesAbiertas() {
        Perfil usuario = perfiles.buscarPorUserId(USUARIO).orElseThrow();
        Publicacion publicacion = publicada(ana, "Java");
        Solicitud solicitud = Solicitud.crear(usuario, publicacion, publicacion.getFranjas().get(0).getId(),
                "Hola", "Aprender Java");
        Contratacion abierta = new Contratacion(usuario.getId(), ana.getId(), new SkillCoins(10), solicitud);
        contrataciones.guardar(abierta);

        assertThrows(IllegalStateException.class, () -> perfilService.darDeBaja(ANA));
        assertTrue(perfiles.buscarPorUserId(ANA).orElseThrow().isActivo());
        assertEquals(EstadoPublicacion.PUBLICADA, publicacion.getEstado());

        abierta.rechazar(ana.getId());
        perfilService.darDeBaja(ANA);
        assertFalse(perfiles.buscarPorUserId(ANA).orElseThrow().isActivo());
    }

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
        publicacion.reemplazarHabilidades(Set.of(java));
        return publicacion;
    }

    private Publicacion publicada(Perfil autor, String titulo) {
        Publicacion publicacion = borrador(autor, titulo);
        publicacion.publicar(autor.getUserId());
        publicaciones.guardar(publicacion);
        return publicacion;
    }

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
