package com.skillswap.application.pagos;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.compartido.UnidadTransaccional;
import com.skillswap.domain.compartido.SkillCoins;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;

import java.util.concurrent.atomic.AtomicReference;

public class WalletService {

    private final BilleteraRepository billeteras;
    private final UnidadTransaccional transacciones;

    public WalletService(BilleteraRepository billeteras, UnidadTransaccional transacciones) {
        this.billeteras = billeteras;
        this.transacciones = transacciones;
    }

    public int consultarSaldo(String userId) {
        validarUserId(userId);
        Billetera billetera = billeteras.buscarPorTitular(userId);
        return billetera == null ? 0 : billetera.consultarSaldo().getValor();
    }

    public int acreditarDemo(String userId, int importe) {
        validarUserId(userId);
        if (importe <= 0) {
            throw new IllegalArgumentException("El importe debe ser positivo");
        }
        AtomicReference<Integer> resultado = new AtomicReference<>();

        transacciones.ejecutar(() -> {
            Billetera billetera = billeteras.buscarPorTitular(userId);
            if (billetera == null) {
                billetera = new Billetera(userId);
            }
            billetera.acreditar(new SkillCoins(importe));
            billeteras.guardar(billetera);
            resultado.set(billetera.consultarSaldo().getValor());
        });

        return resultado.get();
    }

    public Pagina<Movimiento> listarMovimientosPaginado(
            String userId, int pagina, int tamanio) {

        validarUserId(userId);
        Pagina.validarPedido(pagina, tamanio);

        return billeteras.listarMovimientosPaginado(
                userId, pagina, tamanio
        );
    }

    private void validarUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
    }
}