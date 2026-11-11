package com.skillswap.controllers;

import com.skillswap.application.CalificacionService;

import java.util.UUID;

public class CalificacionController {
    private final CalificacionService calificacionService;

    public CalificacionController(CalificacionService calificacionService) {
        this.calificacionService = calificacionService;
    }

    public void calificar(String userId, UUID contratacionId, String calificadoId, int puntaje) {
        calificacionService.calificar(userId, contratacionId, calificadoId, puntaje);
    }
}
