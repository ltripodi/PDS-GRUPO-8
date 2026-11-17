package com.skillswap.presentation.consola;

import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.RegistroHistorialDTO;
import com.skillswap.application.notificacion.NotificacionDTO;
import com.skillswap.application.pagos.ComprobantePagoDTO;
import com.skillswap.application.pagos.MovimientoDTO;
import com.skillswap.application.perfil.PerfilPublicoDTO;
import com.skillswap.application.publicacion.FranjaDTO;
import com.skillswap.application.publicacion.PublicacionDTO;
import com.skillswap.domain.busqueda.TipoRecomendacion;
import com.skillswap.presentation.contrato.Controladores;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;

/*
 * Interfaz de consola para la demo. Solo habla con los controllers y solo recibe DTOs:
 * nunca toca entidades, repositorios ni JPA. Todos los errores llegan como ErrorAplicacion.
 *
 * Login simulado: se elige un userId de los datos demo (V2__datos_demo.sql).
 */
public final class ConsolaSkillSwap {

    private static final List<String> USUARIOS_DEMO = List.of("ana", "bruno", "carla", "diego", "uade-admin");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());

    private final Controladores app;
    private final Scanner entrada;
    private String usuario;
    /* Ultimo listado mostrado: el usuario elige por numero en vez de tipear UUIDs. */
    private List<PublicacionDTO> ultimasPublicaciones = List.of();

    public ConsolaSkillSwap(Controladores app, Scanner entrada) {
        this.app = app;
        this.entrada = entrada;
    }

    /* La arranca SkillSwapApplication.main despues de armar los controllers. */
    public void ejecutar() {
        System.out.println("\n=== SkillSwap ===");
        iniciarSesion();
        while (true) {
            mostrarMenu();
            String opcion = leer("Opcion").toUpperCase();
            if (opcion.equals("0")) {
                System.out.println("Hasta luego.");
                return;
            }
            try {
                atender(opcion);
            } catch (ErrorAplicacion e) {
                System.out.println("  [" + e.getTipo() + "] " + e.getMessage());
            } catch (IllegalArgumentException e) {
                // Entrada mal tipeada en la consola (numero o UUID invalido): no llego a ningun caso de uso.
                System.out.println("  Entrada invalida: " + e.getMessage());
            }
        }
    }

    private void mostrarMenu() {
        System.out.println("\n--- Usuario: " + usuario + " ---");
        System.out.println(" 1 Buscar publicaciones          2 Recomendaciones para mi");
        System.out.println(" 3 Solicitar una publicacion     4 Mis contrataciones");
        System.out.println(" 5 Aceptar solicitud             6 Rechazar solicitud");
        System.out.println(" 7 Pagar                         8 Iniciar sesion de trabajo");
        System.out.println(" 9 Completar                    10 Cancelar");
        System.out.println("11 Proponer reprogramacion      12 Aceptar reprogramacion");
        System.out.println("13 Rechazar reprogramacion      14 Historial de una contratacion");
        System.out.println("15 Calificar a la otra parte    16 Comprobante de pago");
        System.out.println("17 Mi billetera                 18 Mis notificaciones");
        System.out.println("19 Mi perfil                    20 Mis publicaciones");
        System.out.println(" U Cambiar de usuario            D Demo guiada (flujo completo)");
        System.out.println(" 0 Salir");
    }

    private void atender(String opcion) {
        switch (opcion) {
            case "1" -> buscar();
            case "2" -> recomendar();
            case "3" -> solicitar();
            case "4" -> listarContrataciones();
            case "5" -> app.contrataciones().aceptar(usuario, elegirContratacion());
            case "6" -> app.contrataciones().rechazar(usuario, elegirContratacion());
            case "7" -> app.contrataciones().pagar(usuario, elegirContratacion());
            case "8" -> app.contrataciones().iniciar(usuario, elegirContratacion());
            case "9" -> app.contrataciones().completar(usuario, elegirContratacion());
            case "10" -> app.contrataciones().cancelar(usuario, elegirContratacion(), leer("Motivo"));
            case "11" -> proponerReprogramacion();
            case "12" -> app.contrataciones().aceptarReprogramacion(usuario, elegirContratacion());
            case "13" -> app.contrataciones().rechazarReprogramacion(usuario, elegirContratacion());
            case "14" -> mostrarHistorial(elegirContratacion());
            case "15" -> calificar();
            case "16" -> mostrarComprobante(elegirContratacion());
            case "17" -> mostrarBilletera();
            case "18" -> mostrarNotificaciones();
            case "19" -> mostrarPerfil();
            case "20" -> mostrarPublicacionesPropias();
            case "U" -> iniciarSesion();
            case "D" -> new DemoGuiada(app).ejecutar();
            default -> {
                System.out.println("  Opcion desconocida.");
                return;
            }
        }
        if (List.of("5", "6", "7", "8", "9", "10", "11", "12", "13", "15").contains(opcion)) {
            System.out.println("  Listo.");
        }
    }

    // ---------- sesion ----------

    private void iniciarSesion() {
        System.out.println("Usuarios demo: " + String.join(", ", USUARIOS_DEMO));
        String elegido = leer("userId");
        while (elegido.isBlank()) {
            elegido = leer("userId");
        }
        usuario = elegido;
    }

    // ---------- publicaciones ----------

    private void buscar() {
        String texto = leer("Texto a buscar (Enter = todas)");
        mostrarPublicaciones(app.busqueda()
                .buscar(new FiltrosBusqueda(texto, null, null, null, null, null, null, null))
                .getElementos());
    }

    private void recomendar() {
        String tipo = leer("1 Por mis habilidades / 2 Populares recientes");
        TipoRecomendacion criterio = tipo.equals("2")
                ? TipoRecomendacion.POPULARIDAD_RECIENTE : TipoRecomendacion.HABILIDADES;
        mostrarPublicaciones(app.recomendaciones().recomendar(usuario, criterio, 0, 20).getElementos());
    }

    private void mostrarPublicaciones(List<PublicacionDTO> publicaciones) {
        ultimasPublicaciones = publicaciones;
        if (publicaciones.isEmpty()) {
            System.out.println("  (sin resultados)");
            return;
        }
        for (int i = 0; i < publicaciones.size(); i++) {
            PublicacionDTO p = publicaciones.get(i);
            System.out.printf("  %d) %s | %s | %s | %d SkillCoins | %d min | %s (rep. %.1f)%n",
                    i + 1, p.getTitulo(), p.getCategoria(), p.getModalidad(), p.getPrecio(),
                    p.getDuracionMinutos(), p.getProveedor(), p.getReputacion());
        }
    }

    private void mostrarPublicacionesPropias() {
        List<PublicacionDTO> propias = app.publicaciones().listarPropias(usuario).getElementos();
        mostrarPublicaciones(propias);
        propias.forEach(p -> System.out.println("     " + p.getTitulo() + " -> " + p.getEstado()));
    }

    private void solicitar() {
        if (ultimasPublicaciones.isEmpty()) {
            System.out.println("  Primero busque publicaciones (opcion 1) para elegir una.");
            return;
        }
        PublicacionDTO publicacion = elegir(ultimasPublicaciones, "Numero de publicacion");
        FranjaDTO franja = elegirFranja(publicacion);
        if (franja == null) {
            return;
        }
        String mensaje = leer("Mensaje para el proveedor");
        String objetivo = leer("Objetivo");
        UUID id = app.contrataciones().solicitar(usuario, publicacion.getId(), mensaje, franja.getId(), objetivo);
        System.out.println("  Solicitud enviada (contratacion " + id + "). Queda PENDIENTE hasta que el proveedor acepte.");
    }

    private FranjaDTO elegirFranja(PublicacionDTO publicacion) {
        List<FranjaDTO> franjas = publicacion.getFranjas();
        if (franjas.isEmpty()) {
            System.out.println("  La publicacion no tiene franjas horarias.");
            return null;
        }
        for (int i = 0; i < franjas.size(); i++) {
            FranjaDTO f = franjas.get(i);
            System.out.printf("  %d) %s a %s (%d lugares libres)%n", i + 1,
                    FORMATO.format(f.getInicio()), FORMATO.format(f.getFin()), f.getLugaresLibres());
        }
        return elegir(franjas, "Numero de franja");
    }

    // ---------- contrataciones ----------

    private List<ContratacionDTO> listarContrataciones() {
        List<ContratacionDTO> propias = app.contrataciones().listarContrataciones(usuario).getElementos();
        if (propias.isEmpty()) {
            System.out.println("  (no tiene contrataciones)");
        }
        for (int i = 0; i < propias.size(); i++) {
            ContratacionDTO c = propias.get(i);
            System.out.printf("  %d) %s | con %s | %d SkillCoins | %s%n",
                    i + 1, c.getTitulo(), c.getContraparte(), c.getPrecio(), c.getEstado());
        }
        return propias;
    }

    private ContratacionDTO elegirContratacionDTO() {
        List<ContratacionDTO> propias = listarContrataciones();
        if (propias.isEmpty()) {
            throw new IllegalArgumentException("no hay contrataciones para elegir");
        }
        return elegir(propias, "Numero de contratacion");
    }

    private UUID elegirContratacion() {
        return elegirContratacionDTO().getId();
    }

    private void proponerReprogramacion() {
        ContratacionDTO contratacion = elegirContratacionDTO();
        PublicacionDTO publicacion = app.busqueda()
                .buscar(new FiltrosBusqueda(contratacion.getTitulo(), null, null, null, null, null, null, null))
                .getElementos().stream()
                .filter(p -> p.getTitulo().equals(contratacion.getTitulo()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("la publicacion ya no esta visible"));
        FranjaDTO nueva = elegirFranja(publicacion);
        if (nueva != null) {
            app.contrataciones().proponerReprogramacion(usuario, contratacion.getId(), nueva.getId());
        }
    }

    private void mostrarHistorial(UUID contratacionId) {
        for (RegistroHistorialDTO r : app.contrataciones().consultarHistorial(usuario, contratacionId).getElementos()) {
            System.out.printf("  %s  %s -> %s  por %s  (%s)%n", r.getFecha(), r.getOrigen(), r.getDestino(),
                    r.getActor(), r.getMotivo());
        }
    }

    private void mostrarComprobante(UUID contratacionId) {
        ComprobantePagoDTO comprobante = app.contrataciones().consultarComprobante(usuario, contratacionId);
        System.out.println("  Pago " + comprobante.getPagoId() + ": " + comprobante.getTotal()
                + " SkillCoins el " + FORMATO.format(comprobante.getFecha()));
    }

    private void calificar() {
        ContratacionDTO contratacion = elegirContratacionDTO();
        int puntaje = Integer.parseInt(leer("Puntaje (1 a 5)"));
        app.calificaciones().calificar(usuario, contratacion.getId(), contratacion.getContraparteUserId(), puntaje);
    }

    // ---------- billetera, notificaciones, perfil ----------

    private void mostrarBilletera() {
        System.out.println("  Saldo: " + app.billetera().consultarSaldo(usuario) + " SkillCoins");
        for (MovimientoDTO m : app.billetera().listarMovimientos(usuario).getElementos()) {
            System.out.printf("  %s  %-22s %6d%n", FORMATO.format(m.getFecha()), m.getTipo(), m.getMonto());
        }
    }

    private void mostrarNotificaciones() {
        List<NotificacionDTO> bandeja = app.notificaciones().listar(usuario).getElementos();
        if (bandeja.isEmpty()) {
            System.out.println("  (sin notificaciones)");
        }
        for (NotificacionDTO n : bandeja) {
            System.out.printf("  %s %s  %s: %s%n", n.isLeida() ? " " : "*", FORMATO.format(n.getFecha()),
                    n.getTitulo(), n.getDetalle());
        }
        System.out.println("  Enviados por los mocks en esta ejecucion: " + app.emailsEnviados() + " emails, "
                + app.pushEnviados() + " push.");
        if (!bandeja.isEmpty() && leer("Marcar todas como leidas? (s/n)").equalsIgnoreCase("s")) {
            app.notificaciones().marcarTodasLeidas(usuario);
        }
    }

    private void mostrarPerfil() {
        PerfilPublicoDTO perfil = app.perfiles().verPerfil(usuario);
        System.out.println("  " + perfil.getNombreVisible() + " (" + perfil.getUserId() + ") - " + perfil.getRol());
        System.out.println("  " + perfil.getBiografia());
        System.out.printf("  Reputacion %.1f con %d resenas | habilidades: %s%n",
                perfil.getReputacion(), perfil.getResenas(), String.join(", ", perfil.getHabilidades()));
    }

    // ---------- utilidades ----------

    private String leer(String pregunta) {
        System.out.print(pregunta + ": ");
        // Se descarta el BOM que agregan algunas consolas de Windows al redirigir la entrada.
        return entrada.hasNextLine() ? entrada.nextLine().replace("﻿", "").trim() : "0";
    }

    private <T> T elegir(List<T> opciones, String pregunta) {
        int numero = Integer.parseInt(leer(pregunta));
        if (numero < 1 || numero > opciones.size()) {
            throw new IllegalArgumentException("elija un numero entre 1 y " + opciones.size());
        }
        return opciones.get(numero - 1);
    }
}
