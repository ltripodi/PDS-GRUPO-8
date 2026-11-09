package com.skillswap.application;

import com.skillswap.domain.EstrategiaRecomendacion;
import com.skillswap.domain.TipoRecomendacion;

import java.time.Duration;
import java.util.List;

public class RecomendacionService {
    private List<EstrategiaRecomendacion> estrategias;
    private PublicacionRepository publicaciones;
    private PerfilRepository perfiles;
    private SolicitudRepository solicitudes;
    private Duration ventanaPopularidad;

    public Pagina<PublicacionDTO> recomendar(String userId, TipoRecomendacion tipo, int pagina, int tamanio) {
        throw new UnsupportedOperationException("Pendiente de implementar");
    }
}
