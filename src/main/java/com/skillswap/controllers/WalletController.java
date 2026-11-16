package com.skillswap.controllers;

import com.skillswap.application.Pagina;
import com.skillswap.application.WalletService;
import com.skillswap.domain.Movimiento;

import java.util.List;

public class WalletController extends ControllerBase {

    private final WalletService walletService;

    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    public int consultarSaldo(String userId) {
        return ejecutar(() -> walletService.consultarSaldo(userId));
    }

    public List<Movimiento> listarMovimientos(String userId) {
        return ejecutar(() -> walletService.listarMovimientos(userId));
    }

    public int acreditarDemo(String userId, int importe) {
        return ejecutar(() -> walletService.acreditarDemo(userId, importe));
    }

    public Pagina<Movimiento> listarMovimientosPaginado(String userId, int pagina, int tamanio) {
        return ejecutar(() -> walletService.listarMovimientosPaginado(userId, pagina, tamanio));
    }
}
