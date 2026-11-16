package com.skillswap.application;

import com.skillswap.domain.CanalNotificacion;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.Notificacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.PreferenciaNotificacion;
import com.skillswap.domain.TipoEvento;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/*
 * Caso de uso de 4.6. Decide a quien avisar, con que texto y por que canales.
 * La entrega la hace cada CanalEnvio (Adapter): el servicio no conoce a los proveedores reales.
 */
public class NotificacionService {

    private static final Logger LOG = Logger.getLogger(NotificacionService.class.getName());

    /* Quien no tiene perfil (por ejemplo, el usuario de una organizacion) recibe solo la notificacion interna. */
    private static final PreferenciaNotificacion PREFERENCIA_POR_DEFECTO =
            new PreferenciaNotificacion(EnumSet.of(CanalNotificacion.INTERNA));

    private final NotificacionRepository notificaciones;
    private final PerfilRepository perfiles;
    private final Map<CanalNotificacion, CanalEnvio> canales;

    public NotificacionService(NotificacionRepository notificaciones, PerfilRepository perfiles,
                               List<CanalEnvio> canales) {
        this.notificaciones = Objects.requireNonNull(notificaciones, "El repositorio de notificaciones es obligatorio");
        this.perfiles = Objects.requireNonNull(perfiles, "El repositorio de perfiles es obligatorio");
        this.canales = indexarPorTipo(canales);
    }

    // ---------- Envio (lo invoca NotificacionesObserver) ----------

    /* Una notificacion por destinatario, entregada por cada canal que ese usuario eligio. */
    public void notificar(EventoDominio evento) {
        if (evento == null) {
            throw new IllegalArgumentException("El evento es obligatorio");
        }
        for (String destinatario : evento.getDestinatarios()) {
            Mensaje mensaje = mensajePara(evento.getTipo(), destinatario.equals(evento.getActorUserId()));
            Notificacion notificacion = new Notificacion(destinatario, evento.getTipo(), mensaje.titulo(),
                    mensaje.detalle(), evento.getOcurridoEn());
            entregar(notificacion, preferenciaDe(destinatario));
        }
    }

    // ---------- Bandeja ----------

    /* La pagina la resuelve el repositorio (en JPA, en la base): no se trae toda la bandeja. */
    public Pagina<NotificacionDTO> listar(String userId, int pagina, int tamanio) {
        validarUserId(userId);
        Pagina.validarPedido(pagina, tamanio);
        return notificaciones.listar(userId, pagina, tamanio)
                .convertir(elementos -> elementos.stream().map(NotificacionService::aDTO).toList());
    }

    public void marcarLeida(String userId, UUID notificacionId) {
        validarUserId(userId);
        if (notificacionId == null) {
            throw new IllegalArgumentException("La notificacion es obligatoria");
        }
        Notificacion notificacion = notificaciones.buscarPorId(notificacionId)
                .orElseThrow(() -> new IllegalArgumentException("No existe la notificacion indicada"));
        if (!notificacion.esDe(userId)) {
            throw new SecurityException("La notificacion pertenece a otro usuario");
        }
        if (!notificacion.isLeida()) {
            notificacion.marcarLeida();
            notificaciones.guardar(notificacion);
        }
    }

    public void marcarTodasLeidas(String userId) {
        validarUserId(userId);
        notificaciones.marcarTodasLeidas(userId);
    }

    // ---------- Preferencias ----------

    public void configurarCanales(String userId, Set<CanalNotificacion> canalesElegidos) {
        validarUserId(userId);
        Perfil perfil = perfiles.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
        perfil.cambiarCanalesNotificacion(canalesElegidos);
        perfiles.guardar(perfil);
    }

    // ---------- Privados ----------

    /*
     * El caso de uso valida su propia entrada: no depende de que lo haga el controller que lo invoca.
     * Sin esto, marcarLeida(null, id) respondia "pertenece a otro usuario", un mensaje engañoso.
     */
    private static void validarUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
    }

    /* Un canal por tipo, y el interno es obligatorio: toda notificacion queda en la bandeja. */
    private static Map<CanalNotificacion, CanalEnvio> indexarPorTipo(List<CanalEnvio> canales) {
        if (canales == null || canales.isEmpty()) {
            throw new IllegalArgumentException("Se requiere al menos un canal de envio");
        }
        Map<CanalNotificacion, CanalEnvio> porTipo = new EnumMap<>(CanalNotificacion.class);
        for (CanalEnvio canal : canales) {
            if (canal == null || canal.tipo() == null) {
                throw new IllegalArgumentException("Cada canal debe existir y declarar su tipo");
            }
            if (porTipo.putIfAbsent(canal.tipo(), canal) != null) {
                throw new IllegalArgumentException("Hay mas de un canal para " + canal.tipo());
            }
        }
        if (!porTipo.containsKey(CanalNotificacion.INTERNA)) {
            throw new IllegalArgumentException("Se requiere el canal interno: toda notificacion queda en la bandeja");
        }
        return porTipo;
    }

    /*
     * Primero se guarda en la bandeja y recien despues salen email y push, segun la preferencia.
     * Si no se pudo guardar, no se manda nada afuera: todo aviso externo tiene su notificacion registrada.
     * Un canal externo que falla no frena a los demas: la operacion de negocio ya se confirmo.
     */
    private void entregar(Notificacion notificacion, PreferenciaNotificacion preferencia) {
        if (!enviarPor(canales.get(CanalNotificacion.INTERNA), notificacion)) {
            return;
        }
        canales.values().stream()
                .filter(canal -> canal.tipo() != CanalNotificacion.INTERNA && preferencia.permite(canal.tipo()))
                .forEach(canal -> enviarPor(canal, notificacion));
    }

    private boolean enviarPor(CanalEnvio canal, Notificacion notificacion) {
        try {
            canal.enviar(notificacion);
            return true;
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "No se pudo enviar la notificacion por " + canal.tipo()
                    + " a " + notificacion.getDestinatarioId(), e);
            return false;
        }
    }

    private PreferenciaNotificacion preferenciaDe(String userId) {
        return perfiles.buscarPorUserId(userId)
                .map(Perfil::getPreferenciaNotificacion)
                .orElse(PREFERENCIA_POR_DEFECTO);
    }

    private record Mensaje(String titulo, String detalle) {
    }

    /* Los textos son presentacion: viven aca y no en TipoEvento. Sin default, un tipo nuevo sin texto no compila. */
    private static Mensaje mensajePara(TipoEvento tipo, boolean esActor) {
        return switch (tipo) {
            case SOLICITUD_RECIBIDA -> esActor
                    ? new Mensaje("Tu solicitud fue enviada", "El proveedor tiene que aceptarla o rechazarla.")
                    : new Mensaje("Recibiste una solicitud", "Revisala y acepta o rechaza la solicitud.");
            case ACEPTACION -> esActor
                    ? new Mensaje("Aceptaste la solicitud", "El cliente ya puede confirmar el pago.")
                    : new Mensaje("Tu solicitud fue aceptada", "Confirma el pago con SkillCoins para reservar la sesion.");
            case PAGO_CONFIRMADO -> esActor
                    ? new Mensaje("Tu pago fue confirmado", "La sesion quedo confirmada.")
                    : new Mensaje("El cliente pago la sesion", "La sesion quedo confirmada y paga.");
            case REPROGRAMACION -> new Mensaje(
                    esActor ? "Confirmaste la reprogramacion" : "La sesion fue reprogramada",
                    "La sesion tiene una nueva fecha.");
            case CANCELACION -> new Mensaje(
                    esActor ? "Cancelaste la contratacion" : "La contratacion fue cancelada",
                    "Si correspondia, el reintegro se aplica segun la politica de cancelacion.");
            case SESION_COMPLETADA -> new Mensaje(
                    esActor ? "Marcaste la sesion como completada" : "La sesion fue completada",
                    "Ya podes calificar la sesion.");
        };
    }

    private static NotificacionDTO aDTO(Notificacion notificacion) {
        return new NotificacionDTO(notificacion.getId(), notificacion.getTitulo(), notificacion.getDetalle(),
                notificacion.getFecha(), notificacion.isLeida(), notificacion.getTipo().name());
    }
}