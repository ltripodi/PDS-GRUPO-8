package com.skillswap.application;

import com.skillswap.domain.Billetera;
import com.skillswap.domain.Movimiento;

import java.util.List;

public interface BilleteraRepository {
    Billetera buscarPorTitular(String userId);

    void guardar(Billetera billetera);

    List<Movimiento> listarMovimientos(String userId);
}
