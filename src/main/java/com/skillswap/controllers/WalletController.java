package com.skillswap.controllers;

import com.skillswap.application.Pagina;
import com.skillswap.application.WalletService;
import com.skillswap.domain.Movimiento;

import java.util.List;

public class WalletController {
    private WalletService walletService;

    public int consultarSaldo(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Movimiento> listarMovimientos(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public int acreditarDemo(String userId, int importe) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Movimiento> listarMovimientosPaginado(String userId, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
