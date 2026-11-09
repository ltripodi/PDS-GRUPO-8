package com.skillswap.controllers;

import com.skillswap.application.ComprobantePagoDTO;
import com.skillswap.application.ContratacionService;
import com.skillswap.application.Pagina;
import com.skillswap.application.PagoService;
import com.skillswap.domain.Contratacion;

import java.util.List;
import java.util.UUID;

public class ContratacionController {
    private ContratacionService contratacionService;
    private PagoService pagoService;

    public ComprobantePagoDTO consultarComprobante(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<Contratacion> listarContrataciones(String userId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Contratacion solicitar(String userId, String publicacionId, String objetivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Contratacion avanzar(String userId, String contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Contratacion cancelar(String userId, String contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public UUID solicitarConFranja(String userId, UUID publicacionId, String mensaje, UUID franjaId, String objetivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void aceptar(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void rechazar(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void pagar(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void iniciar(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void completar(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void cancelar(String userId, UUID contratacionId, String motivo) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void proponerReprogramacion(String userId, UUID contratacionId, UUID nuevaFranjaId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void aceptarReprogramacion(String userId, UUID contratacionId) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public Pagina<Contratacion> listarContratacionesPaginado(String userId, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
