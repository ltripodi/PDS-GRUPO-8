package com.skillswap.presentation.controller;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.pagos.MovimientoDTO;
import com.skillswap.application.pagos.WalletService;
import com.skillswap.domain.pagos.Movimiento;

import java.util.List;

/* Devuelve MovimientoDTO: la interfaz nunca conoce la entidad Movimiento. */
public class WalletController extends ControllerBase {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    public int consultarSaldo(String userId) {
        return ejecutar(() -> walletService.consultarSaldo(userId));
    }

    public List<MovimientoDTO> listarMovimientos(String userId) {
        return ejecutar(() -> aDTOs(walletService.listarMovimientos(userId)));
    }

    /*
     * Solo para la demo: carga SkillCoins sin pago real (no hay pasarela en el alcance del TP).
     * En produccion se reemplazaria por la acreditacion que confirme un medio de pago.
     */
    public int acreditarDemo(String userId, int importe) {
        return ejecutar(() -> walletService.acreditarDemo(userId, importe));
    }

    public Pagina<MovimientoDTO> listarMovimientosPaginado(String userId, int pagina, int tamanio) {
        return ejecutar(() -> walletService.listarMovimientosPaginado(userId, pagina, tamanio)
                .convertir(WalletController::aDTOs));
    }

    private static List<MovimientoDTO> aDTOs(List<Movimiento> movimientos) {
        return movimientos.stream()
                .map(m -> new MovimientoDTO(m.getId(), m.getMonto().getValor(), m.getTipo(), m.getFecha()))
                .toList();
    }
}
