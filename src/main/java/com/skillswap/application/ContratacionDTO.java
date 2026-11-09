package com.skillswap.application;

import java.time.LocalDateTime;
import java.util.UUID;

/** DTO. */
public class ContratacionDTO {
    private UUID id;
    private String titulo;
    private String contraparte;
    private LocalDateTime fecha;
    private int precio;
    private EstadoContratacionDTO estado;
}
