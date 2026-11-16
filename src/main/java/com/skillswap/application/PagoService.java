package com.skillswap.application;

import com.skillswap.domain.Billetera;
import com.skillswap.domain.CalculadorPrecio;
import com.skillswap.domain.Contratacion;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.Organizacion;
import com.skillswap.domain.Pago;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.TipoEvento;

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
    private final CalculadorPrecio precio;   // reservado (UML); no se usa en pagar()
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

        Billetera billetera = obtenerOCrearBilletera(clienteUserId);
        billetera.reintegrar(monto);
        billeteras.guardar(billetera);
    }

    // ----------------------------- helpers

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