package com.skillswap.application.pagos;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.domain.pagos.Billetera;
import com.skillswap.domain.pagos.Movimiento;

import java.util.List;

public interface BilleteraRepository {
    Billetera buscarPorTitular(String userId);

    void guardar(Billetera billetera);

    List<Movimiento> listarMovimientos(String userId);
    default Pagina<Movimiento> listarMovimientosPaginado(
            String userId, int pagina, int tamanio) {

        return Pagina.de(
                listarMovimientos(userId),
                pagina,
                tamanio
        );
    }
}
