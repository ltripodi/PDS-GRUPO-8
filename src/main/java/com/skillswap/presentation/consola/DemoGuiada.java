package com.skillswap.presentation.consola;

import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.RegistroHistorialDTO;
import com.skillswap.application.perfil.PerfilPublicoDTO;
import com.skillswap.application.publicacion.DatosPublicacionDTO;
import com.skillswap.application.publicacion.FranjaDTO;
import com.skillswap.application.publicacion.PublicacionDTO;
import com.skillswap.domain.busqueda.OrdenPublicacion;
import com.skillswap.domain.busqueda.TipoRecomendacion;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.TipoPublicacion;
import com.skillswap.presentation.contrato.Controladores;

import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;

/*
 * Recorre los 10 casos de aceptacion obligatorios de la consigna (seccion 15) sobre los datos demo,
 * solo con controllers. Cada paso imprime lo que pasa; si un paso falla, muestra el error y sigue.
 *
 * Cada corrida crea su propia publicacion (con franjas nuevas), asi que se puede repetir sin borrar la base.
 * Usa: ana (proveedora), bruno (cliente con saldo), diego (cliente con 100 SkillCoins) y carla.
 */
public final class DemoGuiada {

    /* IDs fijos de V2. */
    private static final UUID CATEGORIA_TECNOLOGIA = UUID.fromString("01000000-0000-0000-0000-000000000001");
    private static final UUID BORRADOR_SIN_FRANJAS = UUID.fromString("05000000-0000-0000-0000-000000000005");

    private static final int PRECIO_MENTORIA = 150;
    private static final int PRECIO_CONSULTA = 10;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Controladores app;
    private final PrintStream out;
    private int paso;

    DemoGuiada(Controladores app) {
        this(app, System.out);
    }

    /* La ventana Swing la corre con su propia salida y la muestra en un dialogo. */
    public DemoGuiada(Controladores app, PrintStream out) {
        this.app = app;
        this.out = out;
    }

    public void ejecutar() {
        String marca = LocalTime.now().format(HORA);

        // ---------------------------------------------------------------- caso 1
        titulo(1, "ana publica una mentoria valida y bruno la encuentra con una busqueda filtrada");
        PublicacionDTO mentoria = publicar("Code review en vivo (demo " + marca + ")", PRECIO_MENTORIA,
                Duration.ofHours(30), Duration.ofDays(30), Duration.ofDays(31), Duration.ofDays(32));
        if (mentoria == null) {
            out.println("\n  No se pudo crear la publicacion de la demo: no se puede seguir.");
            return;
        }
        out.println("     publicada: \"" + mentoria.getTitulo() + "\" a " + mentoria.getPrecio()
                + " SkillCoins, cupo 1 por franja, " + mentoria.getFranjas().size() + " franjas");
        out.println("     bruno busca \"code review\" en Tecnologia, hasta 200 SkillCoins, de menor a mayor precio:");
        intentar(() -> {
            for (PublicacionDTO p : app.busqueda().buscar(new FiltrosBusqueda("code review", CATEGORIA_TECNOLOGIA,
                    null, null, 200, null, null, OrdenPublicacion.MENOR_PRECIO)).getElementos()) {
                out.println("       - " + p.getTitulo() + " (" + p.getPrecio() + " SkillCoins, " + p.getProveedor() + ")"
                        + (p.getId().equals(mentoria.getId()) ? "   <- la recien publicada" : ""));
            }
        });
        // Por fecha de inicio: la primera es la de dentro de 30 h (caso 6), las otras son de dentro de un mes.
        List<FranjaDTO> franjas = mentoria.getFranjas().stream()
                .sorted(Comparator.comparing(FranjaDTO::getInicio))
                .toList();
        UUID franjaProxima = franjas.get(0).getId();
        UUID franjaPrincipal = franjas.get(1).getId();
        UUID franjaDiego = franjas.get(2).getId();
        UUID franjaUltimoCupo = franjas.get(3).getId();

        // ---------------------------------------------------------------- caso 2
        titulo(2, "Se rechaza publicar una oferta incompleta");
        out.println("     un borrador puede guardarse a medias; lo completo se exige al publicar.");
        out.println("     ana intenta publicar su borrador \"Taller de testing con JUnit\", que no tiene franjas:");
        intentar(() -> app.publicaciones().cambiarEstado("ana", BORRADOR_SIN_FRANJAS.toString(),
                EstadoPublicacion.PUBLICADA));

        // ---------------------------------------------------------------- caso 9
        titulo(9, "Promociones acumulables (Decorator) sin precio negativo");
        out.println("     vigentes (V9): -10 y -5 SkillCoins, se aplican en cadena sobre el precio base al solicitar");
        UUID deBruno = intentar(() -> app.contrataciones().solicitar("bruno", mentoria.getId(),
                "Hola Ana, quiero revisar mi TP", franjaPrincipal, "Mejorar mi codigo"));
        if (deBruno == null) {
            out.println("\n  No se pudo seguir la demo.");
            return;
        }
        out.println("     mentoria: base " + mentoria.getPrecio() + " -> bruno paga " + precio("bruno", deBruno));
        PublicacionDTO consulta = publicar("Consulta express (demo " + marca + ")", PRECIO_CONSULTA,
                Duration.ofDays(33));
        if (consulta != null) {
            UUID express = intentar(() -> app.contrataciones().solicitar("bruno", consulta.getId(),
                    "", consulta.getFranjas().get(0).getId(), "Una duda puntual"));
            if (express != null) {
                out.println("     consulta express: base " + consulta.getPrecio() + " -> " + precio("bruno", express)
                        + " (10 - 10 - 5 queda en 0, no en -5)");
                intentar(() -> app.contrataciones().rechazar("ana", express));
            }
        }

        // ---------------------------------------------------------------- casos 3 y 8
        titulo(3, "ana acepta la solicitud de bruno (State: PENDIENTE -> ACEPTADA) y bruno paga con saldo suficiente");
        int emailsAntes = app.emailsEnviados();
        int pushAntes = app.pushEnviados();
        intentar(() -> app.contrataciones().aceptar("ana", deBruno));
        estado("bruno", deBruno);
        int emails = app.emailsEnviados() - emailsAntes;
        int push = app.pushEnviados() - pushAntes;
        pagar("bruno", deBruno);
        estado("bruno", deBruno);

        titulo(8, "Un mismo evento sale por dos tipos de notificacion (Observer + Adapter)");
        out.println("     la aceptacion de recien genero: " + emails + " email (ana elige email) y "
                + push + " push (bruno elige push), ademas de la bandeja interna");
        intentar(() -> out.println("     bandeja interna de bruno: "
                + app.notificaciones().listar("bruno").getTotalElementos() + " avisos"));

        // ---------------------------------------------------------------- caso 4
        titulo(4, "Dos aceptaciones no pueden consumir el ultimo cupo");
        UUID brunoUltimo = intentar(() -> app.contrataciones().solicitar("bruno", mentoria.getId(),
                "", franjaUltimoCupo, "Segunda sesion"));
        UUID diegoUltimo = intentar(() -> app.contrataciones().solicitar("diego", mentoria.getId(),
                "", franjaUltimoCupo, "Primera sesion"));
        if (brunoUltimo != null && diegoUltimo != null) {
            out.println("     bruno y diego piden la misma franja (cupo 1); ana acepta primero a bruno:");
            intentar(() -> app.contrataciones().aceptar("ana", brunoUltimo));
            estado("bruno", brunoUltimo);
            out.println("     y despues a diego:");
            intentar(() -> app.contrataciones().aceptar("ana", diegoUltimo));
            estado("diego", diegoUltimo);
            out.println("     (si las dos aceptaciones llegan a la vez, el optimistic locking (@Version) rechaza una)");
            intentar(() -> app.contrataciones().rechazar("ana", diegoUltimo));
            intentar(() -> app.contrataciones().cancelar("bruno", brunoUltimo, "Ya no la necesito"));
            out.println("     ana rechaza a diego y bruno cancela la suya: la franja vuelve a quedar libre");
        }

        // ---------------------------------------------------------------- caso 5
        titulo(5, "Un pago con saldo insuficiente no modifica saldos ni estado");
        UUID deDiego = intentar(() -> app.contrataciones().solicitar("diego", mentoria.getId(),
                "Quiero mejorar mi CV", franjaDiego, "Conseguir trabajo"));
        if (deDiego != null) {
            intentar(() -> app.contrataciones().aceptar("ana", deDiego));
            int saldoDiego = app.billetera().consultarSaldo("diego");
            int saldoAna = app.billetera().consultarSaldo("ana");
            out.println("     diego tiene " + saldoDiego + " y la mentoria cuesta " + precio("diego", deDiego) + ":");
            intentar(() -> app.contrataciones().pagar("diego", deDiego));
            out.println("     diego " + saldoDiego + " -> " + app.billetera().consultarSaldo("diego")
                    + " | ana " + saldoAna + " -> " + app.billetera().consultarSaldo("ana"));
            estado("diego", deDiego);
            int reintegro = cancelar("diego", deDiego, "No me alcanza el saldo");
            out.println("     diego cancela (no habia pagado: reintegro " + reintegro + ") y libera el lugar");
        }

        // ---------------------------------------------------------------- caso 6
        titulo(6, "Cancelar y calcular el reintegro con dos politicas (Strategy)");
        out.println("     franja dentro de 30 h. Si cancela el cliente: 100 % con 48 h o mas, 50 % con 24 h, 0 % despues.");
        out.println("     Si cancela el proveedor: el cliente recupera el 100 %.");
        reintegroAlCancelar("bruno", mentoria.getId(), franjaProxima, "a) cancela bruno (cliente)");
        reintegroAlCancelar("ana", mentoria.getId(), franjaProxima, "b) cancela ana (proveedora)");

        // ---------------------------------------------------------------- caso 7
        titulo(7, "Cambiar la estrategia de recomendacion sin tocar el caso de uso");
        recomendaciones("carla", TipoRecomendacion.HABILIDADES, "por habilidades en comun");
        recomendaciones("carla", TipoRecomendacion.POPULARIDAD_RECIENTE, "por solicitudes recientes");

        // ---------------------------------------------------------------- caso 10
        titulo(10, "Completar, calificar una sola vez y recalcular la reputacion");
        intentar(() -> app.contrataciones().iniciar("ana", deBruno));
        intentar(() -> app.contrataciones().completar("ana", deBruno));
        estado("bruno", deBruno);
        intentar(() -> app.calificaciones().calificar("bruno", deBruno, "ana", 5));
        intentar(() -> {
            PerfilPublicoDTO ana = app.perfiles().verPerfil("ana");
            out.printf("     reputacion de ana: %.1f (%d resenas)%n", ana.getReputacion(), ana.getResenas());
        });
        out.println("     bruno intenta calificar otra vez la misma contratacion:");
        intentar(() -> app.calificaciones().calificar("bruno", deBruno, "ana", 1));

        out.println("\n   Historial inmutable de la contratacion de bruno (fecha, actor y motivo):");
        intentar(() -> {
            for (RegistroHistorialDTO r : app.contrataciones().consultarHistorial("bruno", deBruno).getElementos()) {
                out.println("     " + r.getOrigen() + " -> " + r.getDestino() + " por " + r.getActor()
                        + " (" + r.getMotivo() + ")");
            }
        });

        out.println("\nFin de la demo guiada: " + paso + " pasos, los 10 casos de aceptacion.");
    }

    // ------------------------------------------------------------------ pasos compuestos

    /* ana crea un borrador con una franja por cada anticipacion pedida y lo publica. */
    private PublicacionDTO publicar(String titulo, int precio, Duration... anticipaciones) {
        return intentar(() -> {
            PublicacionDTO borrador = app.publicaciones().crearBorrador("ana", datos(titulo, precio));
            Instant ahora = Instant.now().truncatedTo(ChronoUnit.HOURS);
            for (Duration anticipacion : anticipaciones) {
                Instant inicio = ahora.plus(anticipacion);
                app.publicaciones().agregarFranja("ana", borrador.getId(), inicio, inicio.plus(Duration.ofHours(1)));
            }
            return app.publicaciones().cambiarEstado("ana", borrador.getId().toString(), EstadoPublicacion.PUBLICADA);
        });
    }

    private static DatosPublicacionDTO datos(String titulo, int precio) {
        return new DatosPublicacionDTO(titulo, "Revisamos tu codigo juntos y te llevas una lista de mejoras.",
                CATEGORIA_TECNOLOGIA, Modalidad.REMOTA, 60, 1, precio, Set.of("java", "code review"),
                TipoPublicacion.MENTORIA, null);
    }

    /* Solicitar, aceptar, pagar y cancelar en la misma franja; muestra cuanto vuelve al cliente. */
    private void reintegroAlCancelar(String quienCancela, UUID publicacionId, UUID franjaId, String descripcion) {
        out.println("     " + descripcion + ":");
        UUID id = intentar(() -> app.contrataciones().solicitar("bruno", publicacionId, "", franjaId, "Repasar"));
        if (id == null) {
            return;
        }
        intentar(() -> app.contrataciones().aceptar("ana", id));
        pagar("bruno", id);
        int antes = app.billetera().consultarSaldo("bruno");
        int reintegro = cancelar(quienCancela, id, "Imprevisto");
        out.println("        pago " + precio("bruno", id) + ", reintegro " + reintegro + " | bruno " + antes + " -> "
                + app.billetera().consultarSaldo("bruno"));
    }

    private void pagar(String userId, UUID contratacionId) {
        int monto = precio(userId, contratacionId);
        int saldo = app.billetera().consultarSaldo(userId);
        if (saldo < monto) {  // la demo se puede repetir: si a bruno ya no le alcanza, se le carga saldo de demo
            intentar(() -> app.billetera().acreditarDemo(userId, monto - saldo));
            out.println("     (se cargan " + (monto - saldo) + " SkillCoins de demo a " + userId + ")");
        }
        int antesCliente = app.billetera().consultarSaldo(userId);
        int antesAna = app.billetera().consultarSaldo("ana");
        intentar(() -> app.contrataciones().pagar(userId, contratacionId));
        out.println("     " + userId + " paga " + monto + ": " + userId + " " + antesCliente + " -> "
                + app.billetera().consultarSaldo(userId) + " | ana " + antesAna + " -> "
                + app.billetera().consultarSaldo("ana"));
    }

    private int cancelar(String userId, UUID contratacionId, String motivo) {
        Integer reintegro = intentar(() -> app.contrataciones().cancelar(userId, contratacionId, motivo));
        return reintegro == null ? 0 : reintegro;
    }

    private void recomendaciones(String userId, TipoRecomendacion tipo, String criterio) {
        out.println("     para " + userId + ", " + criterio + ":");
        intentar(() -> {
            List<PublicacionDTO> recomendadas = app.recomendaciones().recomendar(userId, tipo, 0, 3).getElementos();
            if (recomendadas.isEmpty()) {
                out.println("       (ninguna)");
            }
            recomendadas.forEach(p -> out.println("       - " + p.getTitulo()));
        });
    }

    // ------------------------------------------------------------------ salida

    private void titulo(int caso, String texto) {
        out.println("\n" + (++paso) + ". [Caso " + caso + "] " + texto);
    }

    private ContratacionDTO contratacion(String userId, UUID contratacionId) {
        return app.contrataciones().listarContrataciones(userId, 0, 100).getElementos().stream()
                .filter(c -> c.getId().equals(contratacionId))
                .findFirst()
                .orElse(null);
    }

    private int precio(String userId, UUID contratacionId) {
        ContratacionDTO c = intentar(() -> contratacion(userId, contratacionId));
        return c == null ? 0 : c.getPrecio();
    }

    private void estado(String userId, UUID contratacionId) {
        ContratacionDTO c = intentar(() -> contratacion(userId, contratacionId));
        if (c != null) {
            out.println("     estado de " + userId + ": " + c.getEstado());
        }
    }

    private void intentar(Runnable accion) {
        intentar(() -> {
            accion.run();
            return Boolean.TRUE;
        });
    }

    private <T> T intentar(Supplier<T> accion) {
        try {
            return accion.get();
        } catch (ErrorAplicacion e) {
            out.println("     rechazado [" + e.getTipo() + "]: " + e.getMessage());
            return null;
        }
    }
}
