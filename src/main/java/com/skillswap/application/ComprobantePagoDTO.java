package com.skillswap.application;

import java.time.Instant;
import java.util.UUID;

/** DTO. */
public class ComprobantePagoDTO {
    private UUID pagoId;
    private UUID contratacionId;
    private int total;
    private Instant fecha;
}
