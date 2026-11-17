package com.skillswap.infrastructure;

import com.skillswap.application.compartido.ConflictoConcurrenciaException;
import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.ContratacionService;
import com.skillswap.application.contratacion.DatosSolicitudDTO;
import com.skillswap.application.pagos.BilleteraRepository;
import com.skillswap.application.pagos.PagoService;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.PoliticaCancelacion;
import com.skillswap.domain.contratacion.PoliticaPorAnticipacion;
import com.skillswap.domain.contratacion.PoliticaSinReintegro;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.precio.Promocion;
import com.skillswap.domain.publicacion.Categoria;
import com.skillswap.domain.publicacion.FranjaHoraria;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.Publicacion;
import com.skillswap.domain.publicacion.TipoPublicacion;
import com.skillswap.infrastructure.evento.PublicadorEventosEnMemoria;
import com.skillswap.infrastructure.persistence.JpaBilleteraRepository;
import com.skillswap.infrastructure.persistence.JpaContratacionRepository;
import com.skillswap.infrastructure.persistence.JpaOrganizacionRepository;
import com.skillswap.infrastructure.persistence.JpaPagoRepository;
import com.skillswap.infrastructure.persistence.JpaPerfilRepository;
import com.skillswap.infrastructure.persistence.JpaPromocionRepository;
import com.skillswap.infrastructure.persistence.JpaPublicacionRepository;
import com.skillswap.infrastructure.persistence.JpaUnidadTransaccional;
import com.skillswap.infrastructure.persistence.JPAUtil;
import com.skillswap.presentation.controller.ContratacionController;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/*
 * Flujo economico real sobre H2 (JPA + transacciones): solicitar -> aceptar -> pagar -> cancelar.
 * Cubre los casos de aceptacion 3, 5, 6 y 9 y los negativos de saldo insuficiente y doble pago.
 * No se cierra JPAUtil aca: la EntityManagerFactory la comparten todas las clases de test.
 */
@DisplayName("Pagos, reintegros y promociones contra JPA")
class PagosIntegracionTest {

    private final PerfilRepository perfiles = new JpaPerfilRepository();
    private final BilleteraRepository billeteras = new JpaBilleteraRepository();
    private final JpaContratacionRepository contratacionesRepo = new JpaContratacionRepository();
    private final JpaPromocionRepository promociones = new JpaPromocionRepository();
    private final UnidadTransaccional unidad = new JpaUnidadTransaccional();
    private final PublicadorEventosEnMemoria publicador = new PublicadorEventosEnMemoria();

    private final PagoService pagoService = new PagoService(contratacionesRepo, billeteras, perfiles,
            new JpaOrganizacionRepository(), unidad, null, publicador, new JpaPagoRepository());

    private final ContratacionService conPoliticaTotal = servicio(
            new PoliticaPorAnticipacion(Duration.ofHours(48), Duration.ofHours(24)));
    private final ContratacionService conPoliticaParcial = servicio(
            new PoliticaPorAnticipacion(Duration.ofDays(30), Duration.ofHours(24)));
    private final ContratacionService sinReintegro = servicio(new PoliticaSinReintegro());
    private final ContratacionController controller = new ContratacionController(conPoliticaTotal, pagoService);

    /* Las promociones son globales: se limpian para no alterar el precio de otros tests de la misma JVM. */
    @AfterEach
    void limpiarPromociones() {
        JPAUtil.transaccion(em -> em.createQuery("DELETE FROM Promocion").executeUpdate());
    }

    private ContratacionService servicio(PoliticaCancelacion politica) {
        return new ContratacionService(contratacionesRepo, new JpaPublicacionRepository(), perfiles,
                new JpaOrganizacionRepository(), unidad, publicador, pagoService, politica, promociones);
    }

    // ---------- pago ----------

    /* Caso de aceptacion 3: solicitar, aceptar y pagar con saldo suficiente. */
    @Test
    void pagarDebitaAlClienteAcreditaAlProveedorYConfirmaLaContratacion() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);

        controller.pagar(cliente.getUserId(), contratacion.getId());

        assertEquals(50, saldo(cliente));
        assertEquals(50, saldo(proveedor));
        assertEquals("CONFIRMADA", estado(contratacion));
        assertEquals(50, pagoService.consultarComprobante(cliente.getUserId(), contratacion.getId()).getTotal());
    }

    /* Caso de aceptacion 5: saldo insuficiente no modifica saldos ni estado. */
    @Test
    void unPagoConSaldoInsuficienteNoModificaSaldosNiEstado() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 10);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> controller.pagar(cliente.getUserId(), contratacion.getId()));

        assertEquals(ErrorAplicacion.Tipo.ESTADO_INVALIDO, error.getTipo());
        assertEquals(10, saldo(cliente));
        assertNull(billeteras.buscarPorTitular(proveedor.getUserId()), "el proveedor no recibio nada");
        assertEquals("ACEPTADA", estado(contratacion));
        assertTrue(new JpaPagoRepository().buscarPorContratacion(contratacion.getId()).isEmpty());
    }

    /* Negativo: doble pago. El segundo intento se rechaza y no cobra de nuevo. */
    @Test
    void noSePuedePagarDosVecesLaMismaContratacion() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 200);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);
        controller.pagar(cliente.getUserId(), contratacion.getId());

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> controller.pagar(cliente.getUserId(), contratacion.getId()));

        assertEquals(ErrorAplicacion.Tipo.ESTADO_INVALIDO, error.getTipo());
        assertEquals(150, saldo(cliente), "se cobro una sola vez");
        assertEquals(50, saldo(proveedor));
    }

    @Test
    void soloElClientePuedePagar() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> controller.pagar(proveedor.getUserId(), contratacion.getId()));

        assertEquals(ErrorAplicacion.Tipo.SIN_PERMISO, error.getTipo());
        assertEquals(100, saldo(cliente));
    }

    /* Antes no habia @Version en Billetera: el segundo commit pisaba el saldo del primero. */
    @Test
    void dosOperacionesSimultaneasSobreLaMismaBilleteraNoSePisan() throws Exception {
        Perfil cliente = nuevoPerfil();
        fondear(cliente, 100);

        CyclicBarrier ambasLeyeron = new CyclicBarrier(2);
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> a = hilos.submit(() -> debitarEnTransaccion(cliente, 100, ambasLeyeron));
            Future<Throwable> b = hilos.submit(() -> debitarEnTransaccion(cliente, 100, ambasLeyeron));
            List<Throwable> errores = new ArrayList<>();
            for (Future<Throwable> f : List.of(a, b)) {
                Throwable error = f.get(30, TimeUnit.SECONDS);
                if (error != null) {
                    errores.add(error);
                }
            }

            assertEquals(1, errores.size(), "una sola debe confirmarse");
            assertTrue(errores.get(0) instanceof ConflictoConcurrenciaException, "fallo por: " + errores.get(0));
        } finally {
            hilos.shutdownNow();
        }

        assertEquals(0, saldo(cliente), "se descontaron 100 una sola vez; nunca queda negativo ni se pierde el debito");
        assertEquals(1, billeteras.listarMovimientos(cliente.getUserId()).stream()
                .filter(m -> "DEBITO".equals(m.getTipo())).count());
    }

    // ---------- cancelacion y reintegro ----------

    /* Caso de aceptacion 6, politica 1: reintegro total. Los SkillCoins se conservan: salen del proveedor. */
    @Test
    void cancelarConReintegroTotalDevuelveLaPlataAlClienteYSeLaQuitaAlProveedor() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);
        controller.pagar(cliente.getUserId(), contratacion.getId());

        conPoliticaTotal.cancelar(cliente.getUserId(), contratacion.getId(), "No puedo asistir");

        assertEquals(100, saldo(cliente));
        assertEquals(0, saldo(proveedor));
        assertEquals(100, saldo(cliente) + saldo(proveedor), "no se crean ni se pierden SkillCoins");
        assertEquals("CANCELADA", estado(contratacion));
    }

    /* Caso de aceptacion 6, politica 2: reintegro parcial del 50 %. */
    @Test
    void cancelarConReintegroParcialDevuelveLaMitad() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);
        controller.pagar(cliente.getUserId(), contratacion.getId());

        conPoliticaParcial.cancelar(cliente.getUserId(), contratacion.getId(), "Cambio de planes");

        assertEquals(75, saldo(cliente));
        assertEquals(25, saldo(proveedor));
        assertEquals(100, saldo(cliente) + saldo(proveedor));
    }

    @Test
    void sinReintegroElProveedorConservaElPago() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);
        controller.pagar(cliente.getUserId(), contratacion.getId());

        sinReintegro.cancelar(cliente.getUserId(), contratacion.getId(), "Cancelo igual");

        assertEquals(50, saldo(cliente));
        assertEquals(50, saldo(proveedor));
        assertEquals("CANCELADA", estado(contratacion));
    }

    /* Si el proveedor ya gasto lo cobrado, el reintegro falla y la cancelacion hace rollback completo. */
    @Test
    void siElProveedorYaGastoLoCobradoLaCancelacionSeRevierteEntera() {
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);
        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);
        controller.pagar(cliente.getUserId(), contratacion.getId());
        unidad.ejecutar(() -> {  // el proveedor gasta lo que cobro (los movimientos son LAZY: va dentro de la transaccion)
            Billetera delProveedor = billeteras.buscarPorTitular(proveedor.getUserId());
            delProveedor.debitar(new SkillCoins(50));
            billeteras.guardar(delProveedor);
        });

        ErrorAplicacion error = assertThrows(ErrorAplicacion.class,
                () -> new ContratacionController(conPoliticaTotal, pagoService)
                        .cancelar(cliente.getUserId(), contratacion.getId(), "Quiero mi plata"));

        assertEquals(ErrorAplicacion.Tipo.ESTADO_INVALIDO, error.getTipo());
        assertEquals(50, saldo(cliente), "el cliente no recibio nada");
        assertEquals(0, saldo(proveedor));
        assertEquals("CONFIRMADA", estado(contratacion), "la contratacion sigue confirmada");
    }

    // ---------- promociones ----------

    /* Caso de aceptacion 9: promociones acumulables que modifican el precio sin dejarlo negativo. */
    @Test
    void lasPromocionesVigentesSeAcumulanSobreElPrecioDeLaContratacion() {
        promociones.guardar(new Promocion(new SkillCoins(10)));
        promociones.guardar(new Promocion(new SkillCoins(15)));
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();
        fondear(cliente, 100);

        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);
        controller.pagar(cliente.getUserId(), contratacion.getId());

        assertEquals(25, contratacion.getPrecio(), "50 - 10 - 15");
        assertEquals(75, saldo(cliente), "se cobro el precio con promociones");
        assertEquals(25, saldo(proveedor));
    }

    @Test
    void lasPromocionesNuncaDejanUnPrecioNegativo() {
        promociones.guardar(new Promocion(new SkillCoins(40)));
        promociones.guardar(new Promocion(new SkillCoins(40)));
        Perfil cliente = nuevoPerfil();
        Perfil proveedor = nuevoPerfil();

        ContratacionDTO contratacion = aceptada(cliente, proveedor, 50);

        assertEquals(0, contratacion.getPrecio());
    }

    @Test
    void sinPromocionesElPrecioEsElBaseDeLaPublicacion() {
        ContratacionDTO contratacion = aceptada(nuevoPerfil(), nuevoPerfil(), 50);

        assertEquals(50, contratacion.getPrecio());
    }

    // ---------- helpers ----------

    private Throwable debitarEnTransaccion(Perfil titular, int monto, CyclicBarrier ambasLeyeron) {
        try {
            unidad.ejecutar(() -> {
                Billetera billetera = billeteras.buscarPorTitular(titular.getUserId());
                try {
                    ambasLeyeron.await(10, TimeUnit.SECONDS);
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                billetera.debitar(new SkillCoins(monto));
                billeteras.guardar(billetera);
            });
            return null;
        } catch (RuntimeException e) {
            return e;
        }
    }

    private Perfil nuevoPerfil() {
        Perfil perfil = new Perfil("user-" + UUID.randomUUID(), "Ana", "", ZoneId.of("UTC"));
        perfiles.guardar(perfil);
        return perfil;
    }

    private void fondear(Perfil perfil, int monto) {
        Billetera billetera = new Billetera(perfil.getUserId());
        billetera.acreditar(new SkillCoins(monto));
        billeteras.guardar(billetera);
    }

    private int saldo(Perfil perfil) {
        Billetera billetera = billeteras.buscarPorTitular(perfil.getUserId());
        return billetera == null ? 0 : billetera.getSaldo().getValor();
    }

    private String estado(ContratacionDTO contratacion) {
        return contratacionesRepo.buscarPorId(contratacion.getId()).orElseThrow().getEstado();
    }

    /* Solicitud del cliente aceptada por el proveedor: la contratacion queda lista para pagar. */
    private ContratacionDTO aceptada(Perfil cliente, Perfil proveedor, int precio) {
        Publicacion publicacion = nuevaPublicacion(proveedor, precio);
        ContratacionDTO contratacion = conPoliticaTotal.solicitar(cliente.getUserId(), new DatosSolicitudDTO(
                publicacion.getId(), publicacion.getFranjas().get(0).getId(), "Hola", "Aprender"));
        conPoliticaTotal.aceptar(proveedor.getUserId(), contratacion.getId());
        return contratacion;
    }

    private Publicacion nuevaPublicacion(Perfil proveedor, int precio) {
        Categoria categoria = new Categoria("cat-" + UUID.randomUUID());
        JPAUtil.transaccion(em -> {
            em.persist(categoria);
            return null;
        });
        Publicacion publicacion = Publicacion.crearOferta(proveedor, TipoPublicacion.MENTORIA, "Mentoria", "desc",
                categoria, Modalidad.REMOTA, new SkillCoins(precio), 60, 2);
        Instant inicio = Instant.now().plus(3, ChronoUnit.DAYS);
        publicacion.agregarFranja(new FranjaHoraria(inicio, inicio.plus(1, ChronoUnit.HOURS), 2));
        publicacion.publicar(proveedor.getUserId());
        new JpaPublicacionRepository().guardar(publicacion);
        return publicacion;
    }
}
