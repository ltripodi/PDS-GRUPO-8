package com.skillswap.domain.perfil;

import com.skillswap.domain.calificacion.Calificacion;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/* Perfiles (4.1): datos profesionales, reputacion derivada y baja logica. */
class PerfilTest {

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
}
