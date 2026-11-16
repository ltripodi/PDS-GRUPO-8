package com.skillswap.application.pagos;

import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;

import java.util.List;

public interface BilleteraRepository {
    Billetera buscarPorTitular(String userId);

    void guardar(Billetera billetera);

    List<Movimiento> listarMovimientos(String userId);
}
