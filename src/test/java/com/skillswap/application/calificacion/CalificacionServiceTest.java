package com.skillswap.application.calificacion;

import com.skillswap.application.DoblesEnMemoria;
import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.domain.calificacion.Calificacion;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.contratacion.Solicitud;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;

import org.junit.jupiter.api.Test;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/* Calificaciones (4.1 y seccion 5): solo participantes, una vez y sobre contrataciones completadas. */
class CalificacionServiceTest {

    @Test
    void calificacionVerificaParticipacionUnicidadYRecalculaReputacion() {
        DoblesEnMemoria.Perfiles perfiles = new DoblesEnMemoria.Perfiles();
        Perfil cliente = new Perfil("cliente", "Cliente", "", ZoneId.of("UTC"));
        Perfil proveedor = new Perfil("proveedor", "Proveedor", "", ZoneId.of("UTC"));
        Perfil externo = new Perfil("externo", "Externo", "", ZoneId.of("UTC"));
        perfiles.guardar(cliente);
        perfiles.guardar(proveedor);
        perfiles.guardar(externo);

        Contratacion contratacion = new ContratacionCompletada(cliente, proveedor);
        UnaContratacionRepository contrataciones = new UnaContratacionRepository(contratacion);
        InMemoryCalificacionRepository calificaciones = new InMemoryCalificacionRepository();
        CalificacionService servicio = new CalificacionService(calificaciones, contrataciones, perfiles,
                new UnaOrganizacionRepository(new Organizacion("Otra", "otro")), Runnable::run);

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
        DoblesEnMemoria.Perfiles perfiles = new DoblesEnMemoria.Perfiles();
        Perfil cliente = new Perfil("cliente", "Cliente", "", ZoneId.of("UTC"));
        perfiles.guardar(cliente);
        Organizacion organizacion = new Organizacion("UADE", "uade-admin");
        Contratacion contratacion = new ContratacionCompletada(cliente, organizacion.getId());
        InMemoryCalificacionRepository calificaciones = new InMemoryCalificacionRepository();
        CalificacionService servicio = new CalificacionService(calificaciones,
                new UnaContratacionRepository(contratacion), perfiles,
                new UnaOrganizacionRepository(organizacion), Runnable::run);

        servicio.calificar("uade-admin", contratacion.getId(), "cliente", 4);
        servicio.calificar("cliente", contratacion.getId(), "uade-admin", 5);

        assertEquals(2, calificaciones.calificaciones.size());
        assertEquals(4.0, cliente.obtenerReputacion(),
                "la reputacion se deriva solo de calificaciones dirigidas al perfil");
        assertThrows(IllegalStateException.class,
                () -> servicio.calificar("uade-admin", contratacion.getId(), "cliente", 3));
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

    private static final class UnaContratacionRepository implements ContratacionRepository {
        private final Contratacion contratacion;

        private UnaContratacionRepository(Contratacion contratacion) {
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
        public Pagina<Contratacion> listarPorParticipantePaginado(String userId, int pagina, int tamanio) {
            return Pagina.de(listarPorParticipante(userId), pagina, tamanio);
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
