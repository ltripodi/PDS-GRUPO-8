package com.skillswap.controllers;

import com.skillswap.application.Pagina;
import com.skillswap.application.PublicacionDTO;
import com.skillswap.application.RecomendacionService;
import com.skillswap.domain.TipoRecomendacion;

/*
 * Controller de recomendaciones (4.3): valida la entrada y delega en RecomendacionService, que elige
 * la estrategia (Strategy) segun el tipo pedido. Sin pagina explicita, la primera con el tamanio por defecto.
 */
public class RecomendacionController extends ControllerBase {

    private static final int PRIMERA_PAGINA = 0;
    private static final int TAMANIO_POR_DEFECTO = 20;
    private static final TipoRecomendacion RECOMENDACION_POR_DEFECTO = TipoRecomendacion.HABILIDADES;

    private final RecomendacionService recomendacionService;

    public RecomendacionController(RecomendacionService recomendacionService) {
        this.recomendacionService = recomendacionService;
    }

    public Pagina<PublicacionDTO> recomendar(String userId) {
        return recomendar(userId, RECOMENDACION_POR_DEFECTO, PRIMERA_PAGINA, TAMANIO_POR_DEFECTO);
    }

    public Pagina<PublicacionDTO> recomendar(String userId, TipoRecomendacion tipo, int pagina, int tamanio) {
        return ejecutar(() -> {
            if (userId == null || userId.isBlank()) {
                throw new IllegalArgumentException("El userId es obligatorio");
            }
            if (tipo == null) {
                throw new IllegalArgumentException("El tipo de recomendacion es obligatorio");
            }
            return recomendacionService.recomendar(userId, tipo, pagina, tamanio);
        });
    }
}
