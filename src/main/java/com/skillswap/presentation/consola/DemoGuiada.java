package com.skillswap.presentation.consola;

import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.contratacion.RegistroHistorialDTO;
import com.skillswap.application.perfil.PerfilPublicoDTO;
import com.skillswap.application.publicacion.PublicacionDTO;
import com.skillswap.presentation.contrato.Controladores;

import java.io.PrintStream;
import java.util.UUID;

/*
 * Recorre los casos de aceptacion principales sobre los datos demo (V2__datos_demo.sql), solo con controllers.
 * Cada paso imprime lo que pasa; si un paso falla, se muestra el error y se sigue con el proximo.
 * Cada corrida usa un lugar de la franja: para volver al estado inicial, borrar la base (ver README).
 */
public final class DemoGuiada {

    /* IDs fijos de V2: "Preparacion para entrevistas tecnicas Java" de ana (150 SkillCoins) y sus franjas. */
    private static final UUID MENTORIA_JAVA = UUID.fromString("05000000-0000-0000-0000-000000000001");
    private static final UUID FRANJA_2_MARZO = UUID.fromString("06000000-0000-0000-0000-000000000001");
    private static final UUID FRANJA_4_MARZO = UUID.fromString("06000000-0000-0000-0000-000000000002");

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
        titulo("Busqueda: bruno busca \"java\"");
        intentar(() -> {
            for (PublicacionDTO p : app.busqueda()
                    .buscar(new FiltrosBusqueda("java", null, null, null, null, null, null, null)).getElementos()) {
                out.println("     - " + p.getTitulo() + " (" + p.getPrecio() + " SkillCoins, " + p.getProveedor() + ")");
            }
        });

        titulo("bruno solicita la mentoria de ana para el 2 de marzo");
        UUID deBruno = intentar(() -> app.contrataciones().solicitar("bruno", MENTORIA_JAVA,
                "Hola Ana, tengo una entrevista en dos semanas", FRANJA_2_MARZO, "Practicar entrevista tecnica"));
        if (deBruno == null) {
            out.println("  No se pudo seguir la demo. Borre la base para volver a los datos iniciales.");
            return;
        }
        estado("bruno", deBruno);

        titulo("ana acepta (State: PENDIENTE -> ACEPTADA; Observer: se notifica a bruno)");
        intentar(() -> app.contrataciones().aceptar("ana", deBruno));
        estado("bruno", deBruno);

        titulo("diego tambien solicita, ana acepta, pero diego tiene 100 y la mentoria cuesta 150");
        UUID deDiego = intentar(() -> app.contrataciones().solicitar("diego", MENTORIA_JAVA,
                "Quiero mejorar mi CV", FRANJA_4_MARZO, "Conseguir trabajo"));
        if (deDiego != null) {
            intentar(() -> app.contrataciones().aceptar("ana", deDiego));
            intentar(() -> app.contrataciones().pagar("diego", deDiego));
            out.println("     saldo de diego sigue en " + app.billetera().consultarSaldo("diego"));
            titulo("diego cancela (no pago, no hay reintegro) y libera el lugar");
            intentar(() -> app.contrataciones().cancelar("diego", deDiego, "No me alcanza el saldo"));
            estado("diego", deDiego);
        }

        titulo("bruno paga (SkillCoins: debito al cliente, credito a la proveedora)");
        int antesBruno = app.billetera().consultarSaldo("bruno");
        int antesAna = app.billetera().consultarSaldo("ana");
        intentar(() -> app.contrataciones().pagar("bruno", deBruno));
        out.println("     bruno " + antesBruno + " -> " + app.billetera().consultarSaldo("bruno")
                + " | ana " + antesAna + " -> " + app.billetera().consultarSaldo("ana"));
        estado("bruno", deBruno);

        titulo("ana inicia y completa la sesion");
        intentar(() -> app.contrataciones().iniciar("ana", deBruno));
        intentar(() -> app.contrataciones().completar("ana", deBruno));
        estado("bruno", deBruno);

        titulo("bruno califica a ana con 5 (solo contrataciones completadas)");
        intentar(() -> app.calificaciones().calificar("bruno", deBruno, "ana", 5));
        intentar(() -> {
            PerfilPublicoDTO ana = app.perfiles().verPerfil("ana");
            out.printf("     reputacion de ana: %.1f (%d resenas)%n", ana.getReputacion(), ana.getResenas());
        });

        titulo("bruno intenta calificar dos veces la misma contratacion");
        intentar(() -> app.calificaciones().calificar("bruno", deBruno, "ana", 1));

        titulo("Historial de transiciones de la contratacion de bruno");
        intentar(() -> {
            for (RegistroHistorialDTO r : app.contrataciones().consultarHistorial("bruno", deBruno).getElementos()) {
                out.println("     " + r.getOrigen() + " -> " + r.getDestino() + " por " + r.getActor()
                        + " (" + r.getMotivo() + ")");
            }
        });

        titulo("Notificaciones de bruno");
        intentar(() -> out.println("     " + app.notificaciones().listar("bruno").getTotalElementos()
                + " en su bandeja; mocks en esta ejecucion: " + app.emailsEnviados() + " emails, "
                + app.pushEnviados() + " push"));

        out.println("\nFin de la demo guiada.");
    }

    private void titulo(String texto) {
        out.println("\n" + (++paso) + ". " + texto);
    }

    private void estado(String userId, UUID contratacionId) {
        intentar(() -> app.contrataciones().listarContrataciones(userId).getElementos().stream()
                .filter(c -> c.getId().equals(contratacionId))
                .findFirst()
                .ifPresent(c -> out.println("     estado: " + c.getEstado() + " (" + c.getPrecio() + " SkillCoins)")));
    }

    private void intentar(Runnable accion) {
        intentar(() -> {
            accion.run();
            return Boolean.TRUE;
        });
    }

    private <T> T intentar(java.util.function.Supplier<T> accion) {
        try {
            return accion.get();
        } catch (ErrorAplicacion e) {
            out.println("     rechazado [" + e.getTipo() + "]: " + e.getMessage());
            return null;
        }
    }
}
