package com.skillswap.domain.publicacion;

import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.perfil.Perfil;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/* Publicaciones (4.2): ciclo de vida y autor. */
@DisplayName("4.2 Publicacion")
class PublicacionTest {

    private final Categoria categoria = new Categoria("Programacion");

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
}
