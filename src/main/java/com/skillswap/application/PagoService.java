package com.skillswap.application;

import com.skillswap.domain.CalculadorPrecio;
import com.skillswap.domain.SkillCoins;

import java.util.UUID;

public class PagoService {
    private ContratacionRepository contrataciones;
    private BilleteraRepository billeteras;
    private UnidadTransaccional transacciones;
    private CalculadorPrecio precio;
    private PublicadorEventosPort eventos;
    private PagoRepository pagos;

    public void pagar(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public ComprobantePagoDTO consultarComprobante(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void realizarReintegro(UUID contratacionId, SkillCoins monto) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
