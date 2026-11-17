package com.skillswap.application.pagos;

import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.application.contratacion.ContratacionRepository;
import com.skillswap.application.evento.PublicadorEventosPort;
import com.skillswap.application.perfil.PerfilRepository;
import com.skillswap.application.publicacion.OrganizacionRepository;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.contratacion.Contratacion;
import com.skillswap.domain.evento.EventoDominio;
import com.skillswap.domain.evento.TipoEvento;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Pago;
import com.skillswap.domain.perfil.Perfil;
import com.skillswap.domain.precio.CalculadorPrecio;
import com.skillswap.domain.publicacion.Organizacion;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class PagoService {

    private final ContratacionRepository contrataciones;
    private final BilleteraRepository billeteras;
    private final PerfilRepository perfiles;
    private final OrganizacionRepository organizaciones;
    private final UnidadTransaccional transacciones;
    // Reservado (UML); no se usa en pagar(): el precio con promociones se fija al solicitar
    // (ContratacionService.calcularPrecio) y pagar() cobra Contratacion.getPrecio().
    private final CalculadorPrecio precio;
    private final PublicadorEventosPort eventos;
    private final PagoRepository pagos;

    public PagoService(ContratacionRepository contrataciones,
                       BilleteraRepository billeteras,
                       PerfilRepository perfiles,
                       OrganizacionRepository organizaciones,
                       UnidadTransaccional transacciones,
                       CalculadorPrecio precio,
                       PublicadorEventosPort eventos,
                       PagoRepository pagos) {
        this.contrataciones = contrataciones;
        this.billeteras = billeteras;
        this.perfiles = perfiles;
        this.organizaciones = organizaciones;
        this.transacciones = transacciones;
        this.precio = precio;
        this.eventos = eventos;
        this.pagos = pagos;
    }

    /** UNA transacción: débito + crédito + guardar Pago + confirmar Contratación. */
    public void pagar(String userId, UUID contratacionId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        if (contratacionId == null) {
            throw new IllegalArgumentException("La contratación es obligatoria");
        }

        transacciones.ejecutar(() -> {
            Perfil cliente = perfiles.buscarPorUserId(userId)
                    .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
            Contratacion contratacion = contrataciones.buscarPorId(contratacionId)
                    .orElseThrow(() -> new IllegalArgumentException("No existe la contratación indicada"));

            if (!contratacion.getClienteId().equals(cliente.getId())) {
                throw new SecurityException("Solo el cliente puede pagar esta contratación");
            }
            // Defensa en profundidad: UNIQUE de Pago.contratacionId es la segunda capa.
            if (pagos.buscarPorContratacion(contratacionId).isPresent()) {
                throw new IllegalStateException("La contratación ya tiene un pago registrado");
            }

            // Se valida el estado ANTES de mover saldos: asi un pago a una contratacion que no esta
            // ACEPTADA no toca ningun saldo ni siquiera con una UnidadTransaccional sin rollback.
            // confirmarPago (abajo) sigue siendo la regla autoritativa del State.
            if (!"ACEPTADA".equals(contratacion.getEstado())) {
                throw new IllegalStateException("Solo se puede pagar una contratacion ACEPTADA (estado actual: "
                        + contratacion.getEstado() + ")");
            }

            SkillCoins monto = contratacion.getPrecio();

            Billetera billeteraCliente = obtenerOCrearBilletera(cliente.getUserId());
            billeteraCliente.debitar(monto);
            billeteras.guardar(billeteraCliente);

            String proveedorUserId = resolverUserIdDelProveedor(contratacion.getProveedorId());
            Billetera billeteraProveedor = obtenerOCrearBilletera(proveedorUserId);
            billeteraProveedor.acreditar(monto);
            billeteras.guardar(billeteraProveedor);

            Pago pago = new Pago(contratacionId, monto);
            pagos.guardar(pago);

            contratacion.confirmarPago(cliente.getId());  // State valida ACEPTADA → CONFIRMADA
            contrataciones.guardar(contratacion);

            Set<String> destinatarios = new HashSet<>();
            destinatarios.add(cliente.getUserId());
            destinatarios.add(proveedorUserId);
            eventos.publicarDespuesDelCommit(new EventoDominio(
                    TipoEvento.PAGO_CONFIRMADO, contratacionId, userId, destinatarios));
        });
    }

    public ComprobantePagoDTO consultarComprobante(String userId, UUID contratacionId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        if (contratacionId == null) {
            throw new IllegalArgumentException("La contratación es obligatoria");
        }

        Perfil perfil = perfiles.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
        Contratacion contratacion = contrataciones.buscarPorId(contratacionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la contratación indicada"));

        boolean participa = contratacion.esParticipante(perfil.getId())
                || organizaciones.buscarPorId(contratacion.getProveedorId())
                        .map(o -> userId.equals(o.getUserIdAutorizado()))
                        .orElse(false);
        if (!participa) {
            throw new SecurityException("El usuario no participa de la contratación");
        }

        Pago pago = pagos.buscarPorContratacion(contratacionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe pago para la contratación"));

        return new ComprobantePagoDTO(pago.getId(), pago.getContratacionId(),
                pago.getTotal().getValor(), pago.getFecha());
    }

    /** Se invoca DENTRO de la transacción de ContratacionService.cancelar → NO abre transacción propia. */
    public void realizarReintegro(UUID contratacionId, SkillCoins monto) {
        if (contratacionId == null || monto == null) {
            throw new IllegalArgumentException("Contratación y monto son obligatorios");
        }

        Pago pago = pagos.buscarPorContratacion(contratacionId)
                .orElseThrow(() -> new IllegalStateException("No hay pago para reintegrar"));
        pago.registrarReintegro(monto);
        pagos.guardar(pago);

        Contratacion contratacion = contrataciones.buscarPorId(contratacionId)
                .orElseThrow(() -> new IllegalStateException("No existe la contratación"));
        String clienteUserId = perfiles.buscarPorId(contratacion.getClienteId())
                .map(Perfil::getUserId)
                .orElseThrow(() -> new IllegalStateException("No existe el Perfil del cliente"));

        // El proveedor ya cobro en pagar(): el reintegro sale de su billetera. Si no, se crearian
        // SkillCoins de la nada. Si el proveedor ya gasto ese saldo, debitar() falla con
        // "Saldo insuficiente" y toda la cancelacion hace rollback (no se pierde ni se inventa dinero).
        String proveedorUserId = resolverUserIdDelProveedor(contratacion.getProveedorId());
        Billetera billeteraProveedor = obtenerOCrearBilletera(proveedorUserId);
        billeteraProveedor.debitar(monto);
        billeteras.guardar(billeteraProveedor);

        Billetera billetera = obtenerOCrearBilletera(clienteUserId);
        billetera.reintegrar(monto);
        billeteras.guardar(billetera);
    }

    private Billetera obtenerOCrearBilletera(String userId) {
        Billetera b = billeteras.buscarPorTitular(userId);
        return b != null ? b : new Billetera(userId);
    }

    private String resolverUserIdDelProveedor(UUID proveedorId) {
        Optional<String> perfil = perfiles.buscarPorId(proveedorId).map(Perfil::getUserId);
        if (perfil.isPresent()) {
            return perfil.get();
        }
        return organizaciones.buscarPorId(proveedorId)
                .map(Organizacion::getUserIdAutorizado)
                .orElseThrow(() -> new IllegalStateException("No existe el proveedor de la contratación"));
    }
}