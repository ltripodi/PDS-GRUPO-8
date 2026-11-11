package com.skillswap.application;

import com.skillswap.domain.Contratacion;
import com.skillswap.domain.EventoDominio;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Organizacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.PoliticaCancelacion;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.SkillCoins;
import com.skillswap.domain.Solicitud;
import com.skillswap.domain.TipoEvento;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/*
 * Casos de uso de 4.4 y 4.5. Sin reglas de negocio (estan en el dominio):
 * resuelve el actor, abre la transaccion, publica eventos y devuelve DTOs.
 * Solo depende de puertos.
 */
public class ContratacionService {
    private final ContratacionRepository contrataciones;
    private final PublicacionRepository publicaciones;
    private final PerfilRepository perfiles;
    private final OrganizacionRepository organizaciones;
    private final UnidadTransaccional transacciones;
    private final PublicadorEventosPort eventos;
    private final PagoService pagos;
    private final PoliticaCancelacion politicaCancelacion;

    public ContratacionService(ContratacionRepository contrataciones, PublicacionRepository publicaciones,
                               PerfilRepository perfiles, OrganizacionRepository organizaciones,
                               UnidadTransaccional transacciones, PublicadorEventosPort eventos,
                               PagoService pagos, PoliticaCancelacion politicaCancelacion) {
        this.contrataciones = contrataciones;
        this.publicaciones = publicaciones;
        this.perfiles = perfiles;
        this.organizaciones = organizaciones;
        this.transacciones = transacciones;
        this.eventos = eventos;
        this.pagos = pagos;
        this.politicaCancelacion = politicaCancelacion;
    }

    public ContratacionDTO solicitar(String userId, DatosSolicitudDTO datos) {
        if (datos == null) {
            throw new IllegalArgumentException("Los datos de la solicitud son obligatorios");
        }
        AtomicReference<ContratacionDTO> resultado = new AtomicReference<>();

        transacciones.ejecutar(() -> {
            Perfil cliente = obtenerPerfil(userId);
            Publicacion publicacion = publicaciones.buscarPorId(datos.getPublicacionId())
                    .orElseThrow(() -> new IllegalArgumentException("No existe la publicacion indicada"));

            Solicitud solicitud = Solicitud.crear(cliente, publicacion, datos.getFranjaId(),
                    datos.getMensaje(), datos.getObjetivo());

            Contratacion contratacion = new Contratacion(cliente.getId(), publicacion.getAutorId(),
                    publicacion.getPrecioBase(), solicitud);
            contrataciones.guardar(contratacion); // la solicitud se guarda en cascada

            publicar(TipoEvento.SOLICITUD_RECIBIDA, contratacion, userId);
            resultado.set(aDTO(contratacion, cliente.getId()));
        });

        return resultado.get();
    }

    /* Estado + reserva + cupo en una transaccion; el @Version evita la sobreventa. */
    public void aceptar(String userId, UUID id) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(id);
            contratacion.aceptar(resolverActor(userId, contratacion));
            contrataciones.guardar(contratacion);
            publicar(TipoEvento.ACEPTACION, contratacion, userId);
        });
    }

    public void rechazar(String userId, UUID id) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(id);
            contratacion.rechazar(resolverActor(userId, contratacion));
            contrataciones.guardar(contratacion);
        });
    }

    public void iniciar(String userId, UUID id) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(id);
            contratacion.iniciar(resolverActor(userId, contratacion));
            contrataciones.guardar(contratacion);
        });
    }

    public void completar(String userId, UUID id) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(id);
            contratacion.completar(resolverActor(userId, contratacion));
            contrataciones.guardar(contratacion);
            publicar(TipoEvento.SESION_COMPLETADA, contratacion, userId);
        });
    }

    /*
     * Estado + cupo + reintegro en una transaccion.
     * Strategy: el monto lo decide PoliticaCancelacion; solo si estaba pagada.
     */
    public void cancelar(String userId, UUID contratacionId, String motivo) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(contratacionId);
            boolean estabaPagada = "CONFIRMADA".equals(contratacion.getEstado());
            Duration anticipacion = anticipacionHastaInicio(contratacion);

            contratacion.cancelar(resolverActor(userId, contratacion), motivo);
            contrataciones.guardar(contratacion);

            if (estabaPagada) {
                SkillCoins reintegro = politicaCancelacion.calcularReintegro(contratacion.getPrecio(), anticipacion);
                if (reintegro.getValor() > 0) {
                    pagos.realizarReintegro(contratacion.getId(), reintegro);
                }
            }

            publicar(TipoEvento.CANCELACION, contratacion, userId);
        });
    }

    public void proponerReprogramacion(String userId, UUID contratacionId, UUID nuevaFranjaId) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(contratacionId);
            FranjaHoraria nuevaFranja = contratacion.getSolicitud().getPublicacion().getFranjas().stream()
                    .filter(franja -> franja.getId().equals(nuevaFranjaId))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("La nueva franja debe pertenecer a la publicacion"));

            contratacion.proponerReprogramacion(resolverActor(userId, contratacion), nuevaFranja);
            contrataciones.guardar(contratacion);
        });
    }

    public void aceptarReprogramacion(String userId, UUID contratacionId) {
        transacciones.ejecutar(() -> {
            Contratacion contratacion = obtenerContratacion(contratacionId);
            contratacion.aplicarReprogramacion(resolverActor(userId, contratacion));
            contrataciones.guardar(contratacion);
            publicar(TipoEvento.REPROGRAMACION, contratacion, userId);
        });
    }

    /* Dentro de la transaccion para poder navegar relaciones LAZY al armar el DTO. */
    public List<ContratacionDTO> listar(String userId) {
        AtomicReference<List<ContratacionDTO>> resultado = new AtomicReference<>();

        transacciones.ejecutar(() -> {
            UUID actorId = obtenerPerfil(userId).getId();
            resultado.set(contrataciones.listarPorParticipante(userId).stream()
                    .map(contratacion -> aDTO(contratacion, actorId))
                    .collect(Collectors.toList()));
        });

        return resultado.get();
    }

    /* Consigna 4.3: listados paginados. */
    public Pagina<ContratacionDTO> listarPaginado(String userId, int pagina, int tamanio) {
        if (pagina < 0 || tamanio <= 0) {
            throw new IllegalArgumentException("La pagina debe ser >= 0 y el tamanio > 0");
        }
        List<ContratacionDTO> todas = listar(userId);

        int desde = Math.min(pagina * tamanio, todas.size());
        int hasta = Math.min(desde + tamanio, todas.size());

        return new Pagina<>(todas.subList(desde, hasta), pagina, tamanio, todas.size());
    }

    private Perfil obtenerPerfil(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
        return perfiles.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
    }

    private Contratacion obtenerContratacion(UUID id) {
        return contrataciones.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la contratacion indicada"));
    }

    /* userId -> id de Perfil (u Organizacion en desafios). Los permisos los valida el dominio. */
    private UUID resolverActor(String userId, Contratacion contratacion) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }

        UUID perfilId = perfiles.buscarPorUserId(userId).map(Perfil::getId).orElse(null);
        if (perfilId != null && contratacion.esParticipante(perfilId)) {
            return perfilId;
        }

        return organizaciones.buscarPorId(contratacion.getProveedorId())
                .filter(organizacion -> userId.equals(organizacion.getUserIdAutorizado()))
                .map(Organizacion::getId)
                .orElseThrow(() -> new SecurityException("El usuario no participa de la contratacion"));
    }

    private Duration anticipacionHastaInicio(Contratacion contratacion) {
        if (contratacion.getReserva() == null) {
            return Duration.ZERO;
        }
        Instant inicio = contratacion.getReserva().getFranja().getInicio();
        return Duration.between(Instant.now(), inicio);
    }

    /* Observer: solo publica; los canales los resuelve el modulo de notificaciones. */
    private void publicar(TipoEvento tipo, Contratacion contratacion, String actorUserId) {
        eventos.publicarDespuesDelCommit(new EventoDominio(tipo, contratacion.getId(), actorUserId,
                userIdsDeLasPartes(contratacion)));
    }

    private Set<String> userIdsDeLasPartes(Contratacion contratacion) {
        Set<String> userIds = new HashSet<>();
        perfiles.buscarPorId(contratacion.getClienteId()).ifPresent(perfil -> userIds.add(perfil.getUserId()));
        perfiles.buscarPorId(contratacion.getProveedorId()).ifPresent(perfil -> userIds.add(perfil.getUserId()));
        organizaciones.buscarPorId(contratacion.getProveedorId())
                .ifPresent(organizacion -> userIds.add(organizacion.getUserIdAutorizado()));
        return userIds;
    }

    private ContratacionDTO aDTO(Contratacion contratacion, UUID actorId) {
        UUID contraparteId = contratacion.getClienteId().equals(actorId)
                ? contratacion.getProveedorId()
                : contratacion.getClienteId();

        return new ContratacionDTO(
                contratacion.getId(),
                contratacion.getSolicitud().getPublicacion().getTitulo(),
                nombreDe(contraparteId),
                LocalDateTime.ofInstant(contratacion.getSolicitud().getFechaCreacion(), ZoneId.systemDefault()),
                contratacion.getPrecio().getValor(),
                EstadoContratacionDTO.valueOf(contratacion.getEstado())
        );
    }

    private String nombreDe(UUID parteId) {
        return perfiles.buscarPorId(parteId)
                .map(Perfil::getNombreVisible)
                .or(() -> organizaciones.buscarPorId(parteId).map(Organizacion::getNombre))
                .orElse("Desconocido");
    }
}
