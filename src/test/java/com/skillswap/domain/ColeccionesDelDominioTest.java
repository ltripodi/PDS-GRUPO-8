package com.skillswap.domain;

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
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/* Encapsulamiento comun a todas las entidades: ninguna coleccion se modifica desde afuera. */
class ColeccionesDelDominioTest {

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
}
