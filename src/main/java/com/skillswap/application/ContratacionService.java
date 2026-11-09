package com.skillswap.application;

import com.skillswap.domain.PoliticaCancelacion;

import java.util.List;
import java.util.UUID;

public class ContratacionService {
    private ContratacionRepository contrataciones;
    private PublicacionRepository publicaciones;
    private UnidadTransaccional transacciones;
    private PublicadorEventosPort eventos;
    private SolicitudRepository solicitudes;
    // TODO: private ServicioIdentidad identidad; (tipo ServicioIdentidad no definido en el .mdj)
    private PagoService pagos;
    private PoliticaCancelacion politicaCancelacion;

    public ContratacionDTO solicitar(String userId, DatosSolicitudDTO datos) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void aceptar(String userId, UUID id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void rechazar(String userId, UUID id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void iniciar(String userId, UUID id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public void completar(String userId, UUID id) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }

    public List<ContratacionDTO> listar(String userId) {
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

    public Pagina<ContratacionDTO> listarPaginado(String userId, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
