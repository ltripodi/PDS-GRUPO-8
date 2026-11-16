package com.skillswap.infrastructure;

import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.ContratacionService;
import com.skillswap.application.contratacion.DatosSolicitudDTO;
import com.skillswap.application.contratacion.EstadoContratacionDTO;
import com.skillswap.application.pagos.BilleteraRepository;
import com.skillswap.application.pagos.PagoService;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.PoliticaSinReintegro;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Organizacion;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.infrastructure.evento.PublicadorEventosEnMemoria;
import com.skillswap.infrastructure.persistence.JPAUtil;
import com.skillswap.infrastructure.persistence.JpaBilleteraRepository;
import com.skillswap.infrastructure.persistence.JpaContratacionRepository;
import com.skillswap.infrastructure.persistence.JpaOrganizacionRepository;
import com.skillswap.infrastructure.persistence.JpaPagoRepository;
import com.skillswap.infrastructure.persistence.JpaPerfilRepository;
import com.skillswap.infrastructure.persistence.JpaPromocionRepository;
import com.skillswap.infrastructure.persistence.JpaPublicacionRepository;
import com.skillswap.infrastructure.persistence.JpaUnidadTransaccional;
import com.skillswap.presentation.controller.ContratacionController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Desafio de punta a punta contra JPA: en un desafio el proveedor es una organizacion, que no tiene perfil.
 * Su representante tiene que ver la solicitud en su lista, poder aceptarla y cobrar.
 */
@DisplayName("Desafio de una organizacion contra JPA")
class DesafioIntegracionTest {

    private final PerfilRepository perfiles = new JpaPerfilRepository();
    private final BilleteraRepository billeteras = new JpaBilleteraRepository();
    private final JpaContratacionRepository contrataciones = new JpaContratacionRepository();
    private final JpaOrganizacionRepository organizaciones = new JpaOrganizacionRepository();
    private final UnidadTransaccional unidad = new JpaUnidadTransaccional();
    private final PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();
    private final PagoService pagos = new PagoService(contrataciones, billeteras, perfiles, organizaciones, unidad,
            null, publicador, new JpaPagoRepository());
    private final ContratacionService servicio = new ContratacionService(contrataciones,
            new JpaPublicacionRepository(), perfiles, organizaciones, unidad, publicador, pagos,
            new PoliticaSinReintegro(), new JpaPromocionRepository());
    private final ContratacionController controller = new ContratacionController(servicio, pagos);

    @Test
    void elRepresentanteVeLaSolicitudDelDesafioLaAceptaYCobra() {
        String representante = "rep-" + UUID.randomUUID();
        Organizacion organizacion = new Organizacion("Org " + representante, representante);
        Perfil profesional = nuevoPerfil("Pro");
        fondear(profesional, 500);
        Publicacion desafio = desafioPublicado(organizacion, 300);

        UUID id = controller.solicitar(profesional.getUserId(), desafio.getId(), "Me interesa",
                desafio.getFranjas().get(0).getId(), "Ganar el desafio");

        // El representante (sin perfil) ve la solicitud, con el profesional como contraparte
        List<ContratacionDTO> delRepresentante = controller.listarContrataciones(representante);
        assertEquals(1, delRepresentante.size());
        assertEquals(id, delRepresentante.get(0).getId());
        assertEquals("Pro", delRepresentante.get(0).getContraparte());
        assertEquals(profesional.getUserId(), delRepresentante.get(0).getContraparteUserId());

        // El profesional ve a la organizacion como contraparte
        ContratacionDTO delProfesional = controller.listarContrataciones(profesional.getUserId()).get(0);
        assertEquals(organizacion.getNombre(), delProfesional.getContraparte());
        assertEquals(representante, delProfesional.getContraparteUserId());

        controller.aceptar(representante, id);
        controller.pagar(profesional.getUserId(), id);

        assertEquals(EstadoContratacionDTO.CONFIRMADA, controller.listarContrataciones(representante).get(0).getEstado());
        assertEquals(200, billeteras.buscarPorTitular(profesional.getUserId()).getSaldo().getValor());
        assertEquals(300, billeteras.buscarPorTitular(representante).getSaldo().getValor(),
                "el pago del desafio lo cobra la organizacion, en la billetera de su representante");
    }

    @Test
    void quienNoEsPerfilNiRepresentaUnaOrganizacionNoPuedeListar() {
        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> controller.listarContrataciones("nadie-" + UUID.randomUUID()));

        assertEquals(ErrorAplicacion.Tipo.DATOS_INVALIDOS, error.getTipo());
    }

    @Test
    void elRepresentanteNoVeContratacionesDeOtrasOrganizaciones() {
        String representante = "rep-" + UUID.randomUUID();
        Organizacion propia = new Organizacion("Propia " + representante, representante);
        Organizacion otra = new Organizacion("Otra " + representante, "otro-" + representante);
        Perfil profesional = nuevoPerfil("Pro");
        Publicacion desafioPropio = desafioPublicado(propia, 0);
        Publicacion desafioAjeno = desafioPublicado(otra, 0);

        controller.solicitar(profesional.getUserId(), desafioPropio.getId(), "", desafioPropio.getFranjas().get(0).getId(),
                "Participar");
        controller.solicitar(profesional.getUserId(), desafioAjeno.getId(), "", desafioAjeno.getFranjas().get(0).getId(),
                "Participar");

        List<ContratacionDTO> lista = controller.listarContrataciones(representante);
        assertEquals(1, lista.size());
        assertTrue(lista.get(0).getTitulo().equals(desafioPropio.getTitulo()));
    }

    private Perfil nuevoPerfil(String nombre) {
        Perfil perfil = new Perfil("user-" + UUID.randomUUID(), nombre, "", ZoneId.of("UTC"));
        perfiles.guardar(perfil);
        return perfil;
    }

    private void fondear(Perfil perfil, int monto) {
        Billetera billetera = new Billetera(perfil.getUserId());
        billetera.acreditar(new SkillCoins(monto));
        billeteras.guardar(billetera);
    }

    /* La organizacion se guarda en la misma transaccion que su desafio publicado. */
    private Publicacion desafioPublicado(Organizacion organizacion, int precio) {
        Categoria categoria = new Categoria("cat-" + UUID.randomUUID());
        Publicacion desafio = Publicacion.crearDesafio(organizacion, "Desafio " + UUID.randomUUID(), "desc",
                categoria, Modalidad.REMOTA, new SkillCoins(precio), 120, 3);
        Instant inicio = Instant.now().plus(5, ChronoUnit.DAYS);
        desafio.agregarFranja(new FranjaHoraria(inicio, inicio.plus(2, ChronoUnit.HOURS), 3));
        desafio.publicar(organizacion.getUserIdAutorizado());
        JPAUtil.transaccion(em -> {
            em.persist(categoria);
            em.persist(organizacion);
            em.persist(desafio);
            return null;
        });
        return desafio;
    }
}
