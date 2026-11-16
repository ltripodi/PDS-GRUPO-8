package com.skillswap.presentation.controller;

import com.skillswap.application.compartido.Pagina;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.ContratacionService;
import com.skillswap.application.contratacion.DatosSolicitudDTO;
import com.skillswap.application.contratacion.RegistroHistorialDTO;
import com.skillswap.application.pagos.ComprobantePagoDTO;
import com.skillswap.application.pagos.PagoService;

import java.util.List;
import java.util.UUID;

/*
 * Controller delgado: valida la entrada, delega en el caso de uso y devuelve DTOs.
 * Todo error sale como ErrorAplicacion (ver ControllerBase).
 */
public class ContratacionController extends ControllerBase {
    private final ContratacionService contratacionService;
    private final PagoService pagoService;

    public ContratacionController(ContratacionService contratacionService, PagoService pagoService) {
        this.contratacionService = contratacionService;
        this.pagoService = pagoService;
    }

    public UUID solicitar(String userId, UUID publicacionId, String mensaje, UUID franjaId, String objetivo) {
        return ejecutar(() -> {
            validarUserId(userId);
            requerido(publicacionId, "La publicacion");
            requerido(franjaId, "La franja");

            DatosSolicitudDTO datos = new DatosSolicitudDTO(publicacionId, franjaId, mensaje, objetivo);
            return contratacionService.solicitar(userId, datos).getId();
        });
    }

    public void aceptar(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            contratacionService.aceptar(userId, contratacionId);
        });
    }

    public void rechazar(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            contratacionService.rechazar(userId, contratacionId);
        });
    }

    /* Lo resuelve PagoService (integrante 4). */
    public void pagar(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            pagoService.pagar(userId, contratacionId);
        });
    }

    public void iniciar(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            contratacionService.iniciar(userId, contratacionId);
        });
    }

    public void completar(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            contratacionService.completar(userId, contratacionId);
        });
    }

    public void cancelar(String userId, UUID contratacionId, String motivo) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            if (motivo == null || motivo.isBlank()) {
                throw new IllegalArgumentException("La cancelacion requiere un motivo");
            }
            contratacionService.cancelar(userId, contratacionId, motivo);
        });
    }

    public void proponerReprogramacion(String userId, UUID contratacionId, UUID nuevaFranjaId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            requerido(nuevaFranjaId, "La nueva franja");
            contratacionService.proponerReprogramacion(userId, contratacionId, nuevaFranjaId);
        });
    }

    public void aceptarReprogramacion(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            contratacionService.aceptarReprogramacion(userId, contratacionId);
        });
    }

    public void rechazarReprogramacion(String userId, UUID contratacionId) {
        ejecutarAccion(() -> {
            validar(userId, contratacionId);
            contratacionService.rechazarReprogramacion(userId, contratacionId);
        });
    }

    public List<RegistroHistorialDTO> consultarHistorial(String userId, UUID contratacionId) {
        return ejecutar(() -> {
            validar(userId, contratacionId);
            return contratacionService.consultarHistorial(userId, contratacionId);
        });
    }

    public List<ContratacionDTO> listarContrataciones(String userId) {
        return ejecutar(() -> {
            validarUserId(userId);
            return contratacionService.listar(userId);
        });
    }

    public Pagina<ContratacionDTO> listarContratacionesPaginado(String userId, int pagina, int tamanio) {
        return ejecutar(() -> {
            validarUserId(userId);
            return contratacionService.listarPaginado(userId, pagina, tamanio);
        });
    }

    /* Lo resuelve PagoService (integrante 4). */
    public ComprobantePagoDTO consultarComprobante(String userId, UUID contratacionId) {
        return ejecutar(() -> {
            validar(userId, contratacionId);
            return pagoService.consultarComprobante(userId, contratacionId);
        });
    }

    // Validacion de entrada uniforme para todas las operaciones.

    private void validar(String userId, UUID contratacionId) {
        validarUserId(userId);
        requerido(contratacionId, "La contratacion");
    }

    private void validarUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
    }

    private void requerido(UUID valor, String nombre) {
        if (valor == null) {
            throw new IllegalArgumentException(nombre + " es obligatoria");
        }
    }
}
