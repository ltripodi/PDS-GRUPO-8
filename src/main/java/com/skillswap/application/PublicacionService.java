package com.skillswap.application;

import com.skillswap.domain.EstadoPublicacion;
import com.skillswap.domain.Categoria;
import com.skillswap.domain.FranjaHoraria;
import com.skillswap.domain.Organizacion;
import com.skillswap.domain.Perfil;
import com.skillswap.domain.Publicacion;
import com.skillswap.domain.TipoPublicacion;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PublicacionService {
    private final PublicacionRepository publicacionRepository;
    private final PerfilRepository perfilRepository;
    private final OrganizacionRepository organizacionRepository;

    public PublicacionService(PublicacionRepository publicacionRepository, PerfilRepository perfilRepository,
                              OrganizacionRepository organizacionRepository) {
        this.publicacionRepository = publicacionRepository;
        this.perfilRepository = perfilRepository;
        this.organizacionRepository = organizacionRepository;
    }

    public List<Publicacion> listarPropias(String userId) {
        validarUserId(userId);
        return publicacionRepository.listarPropias(userId);
    }

    public Publicacion crearBorrador(String userId, DatosPublicacionDTO datos) {
        validarUserId(userId);
        validarDatos(datos);
        Categoria categoria = obtenerCategoria(datos.getCategoriaId());
        Publicacion publicacion;

        if (datos.getTipo() == TipoPublicacion.DESAFIO) {
            if (datos.getOrganizacionId() == null) {
                throw new IllegalArgumentException("Un desafio requiere una organizacion autora");
            }
            Organizacion organizacion = organizacionRepository.buscarPorId(datos.getOrganizacionId())
                    .orElseThrow(() -> new IllegalArgumentException("No existe la organizacion indicada"));
            if (!organizacion.autoriza(userId)) {
                throw new SecurityException("El usuario no puede publicar por esa organizacion");
            }
            publicacion = Publicacion.crearDesafio(organizacion, datos.getTitulo(), datos.getDescripcion(), categoria,
                    datos.getModalidad(), datos.getPrecioBase(), datos.getDuracionMinutos(), datos.getCupo());
        } else {
            Perfil perfil = obtenerPerfilActivo(userId);
            publicacion = Publicacion.crearOferta(perfil, datos.getTipo(), datos.getTitulo(), datos.getDescripcion(),
                    categoria, datos.getModalidad(), datos.getPrecioBase(), datos.getDuracionMinutos(), datos.getCupo());
        }
        publicacion.actualizarEtiquetas(datos.getEtiquetas());
        publicacionRepository.guardar(publicacion);
        return publicacion;
    }

    /** Adaptador para el contrato inicial de la UI, que no exponia tipo, cupo ni id de categoria. */
    public Publicacion crearBorrador(String userId, String titulo, String descripcion, String nombreCategoria,
                                     com.skillswap.domain.Modalidad modalidad, int duracionMinutos, int precio) {
        if (nombreCategoria == null || nombreCategoria.isBlank()) {
            throw new IllegalArgumentException("La categoria es obligatoria");
        }
        Categoria categoria = publicacionRepository.listarCategorias().stream()
                .filter(item -> item.getNombre().equalsIgnoreCase(nombreCategoria.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe la categoria indicada"));
        return crearBorrador(userId, new DatosPublicacionDTO(titulo, descripcion, categoria.getId(), modalidad,
                duracionMinutos, 1, new com.skillswap.domain.SkillCoins(precio), Set.of(),
                TipoPublicacion.MENTORIA, null));
    }

    public void editarBorrador(String userId, UUID id, DatosPublicacionDTO datos) {
        validarDatos(datos);
        Publicacion publicacion = obtenerPublicacion(id);
        validarPermiso(userId, publicacion);
        Categoria categoria = obtenerCategoria(datos.getCategoriaId());
        publicacion.editarDatos(datos.getTitulo(), datos.getDescripcion(), datos.getPrecioBase());
        publicacion.cambiarClasificacion(categoria, datos.getModalidad());
        publicacion.configurarCapacidad(datos.getDuracionMinutos(), datos.getCupo());
        publicacion.actualizarEtiquetas(datos.getEtiquetas());
        publicacionRepository.guardar(publicacion);
    }

    public Publicacion cambiarEstado(String userId, UUID id, EstadoPublicacion estado) {
        if (estado == null) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        Publicacion publicacion = obtenerPublicacion(id);
        validarPermiso(userId, publicacion);
        switch (estado) {
            case PUBLICADA -> publicacion.publicar(userId);
            case PAUSADA -> publicacion.pausar(userId);
            case CERRADA -> publicacion.cerrar(userId);
            case BORRADOR -> throw new IllegalArgumentException("No se puede volver una publicacion a borrador");
            case CANCELADA -> throw new IllegalArgumentException("La cancelacion requiere un motivo");
        }
        publicacionRepository.guardar(publicacion);
        return publicacion;
    }

    public void agregarFranja(String userId, UUID id, Instant inicio, Instant fin) {
        Publicacion publicacion = obtenerPublicacion(id);
        validarPermiso(userId, publicacion);
        publicacion.agregarFranja(new FranjaHoraria(inicio, fin, publicacion.getCupo()));
        publicacionRepository.guardar(publicacion);
    }

    public void cancelar(String userId, UUID id, String motivo) {
        Publicacion publicacion = obtenerPublicacion(id);
        validarPermiso(userId, publicacion);
        publicacion.cancelar(userId, motivo);
        publicacionRepository.guardar(publicacion);
    }

    public Pagina<Publicacion> listarPropiasPaginado(String userId, int pagina, int tamanio) {
        validarUserId(userId);
        validarPaginacion(pagina, tamanio);
        return publicacionRepository.listarPropiasPaginado(userId, pagina, tamanio);
    }

    private void validarPermiso(String userId, Publicacion publicacion) {
        validarUserId(userId);
        if (publicacion.getTipo() == TipoPublicacion.DESAFIO) {
            Organizacion organizacion = organizacionRepository.buscarPorId(publicacion.getAutorId())
                    .orElseThrow(() -> new IllegalStateException("La organizacion autora no existe"));
            if (!organizacion.autoriza(userId)) {
                throw new SecurityException("El usuario no puede modificar esta publicacion");
            }
            return;
        }
        Perfil perfil = obtenerPerfilActivo(userId);
        if (!perfil.getId().equals(publicacion.getAutorId())) {
            throw new SecurityException("El usuario no puede modificar esta publicacion");
        }
    }

    private Publicacion obtenerPublicacion(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("El id de publicacion es obligatorio");
        }
        return publicacionRepository.buscarPorId(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe la publicacion indicada"));
    }

    private Perfil obtenerPerfilActivo(String userId) {
        Perfil perfil = perfilRepository.buscarPorUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el perfil indicado"));
        if (!perfil.isActivo()) {
            throw new IllegalStateException("El perfil se encuentra dado de baja");
        }
        return perfil;
    }

    private Categoria obtenerCategoria(UUID categoriaId) {
        if (categoriaId == null) {
            throw new IllegalArgumentException("La categoria es obligatoria");
        }
        return publicacionRepository.listarCategorias().stream()
                .filter(categoria -> categoria.getId().equals(categoriaId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe la categoria indicada"));
    }

    private static void validarDatos(DatosPublicacionDTO datos) {
        if (datos == null || datos.getTipo() == null || datos.getModalidad() == null || datos.getPrecioBase() == null) {
            throw new IllegalArgumentException("Los datos obligatorios de la publicacion no fueron informados");
        }
    }

    private static void validarUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("El userId es obligatorio");
        }
    }

    private static void validarPaginacion(int pagina, int tamanio) {
        if (pagina < 0 || tamanio <= 0) {
            throw new IllegalArgumentException("La paginacion es invalida");
        }
    }
}
