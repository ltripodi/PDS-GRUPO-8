package com.skillswap.presentation.ui;

import com.skillswap.application.busqueda.CategoriaDTO;
import com.skillswap.application.busqueda.FiltrosBusqueda;
import com.skillswap.application.compartido.ErrorAplicacion;
import com.skillswap.application.contratacion.ContratacionDTO;
import com.skillswap.application.contratacion.EstadoContratacionDTO;
import com.skillswap.application.contratacion.RegistroHistorialDTO;
import com.skillswap.application.notificacion.NotificacionDTO;
import com.skillswap.application.pagos.ComprobantePagoDTO;
import com.skillswap.application.pagos.MovimientoDTO;
import com.skillswap.application.perfil.PerfilPublicoDTO;
import com.skillswap.application.publicacion.DatosPublicacionDTO;
import com.skillswap.application.publicacion.FranjaDTO;
import com.skillswap.application.publicacion.PublicacionDTO;
import com.skillswap.domain.busqueda.TipoRecomendacion;
import com.skillswap.domain.publicacion.EstadoPublicacion;
import com.skillswap.domain.publicacion.Modalidad;
import com.skillswap.domain.publicacion.TipoPublicacion;
import com.skillswap.presentation.consola.DemoGuiada;
import com.skillswap.presentation.contrato.Controladores;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;
import java.util.function.Supplier;

/*
 * Interfaz grafica de SkillSwap (Swing), con el mismo criterio que el ejemplo MagicCafe de la catedra:
 * - solo conoce Controladores y DTOs: nunca entidades, repositorios ni JPA;
 * - cada llamada a un controller corre fuera del hilo de Swing (SwingWorker) y la ventana no se congela;
 * - todo error llega como ErrorAplicacion y se muestra con su tipo.
 */
public final class SkillSwapFrame extends JFrame {

    private static final List<String> USUARIOS = List.of("bruno", "ana", "carla", "diego", "acme-admin");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final DateTimeFormatter FECHA_LOCAL = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private static final Color ACENTO = new Color(37, 99, 235);
    private static final Color ACENTO_SUAVE = new Color(219, 234, 254);
    private static final Color VERDE = new Color(21, 128, 61);
    private static final Color ROJO = new Color(185, 28, 28);
    private static final Color GRIS = new Color(100, 116, 139);

    private static final Set<EstadoContratacionDTO> PAGADAS = EnumSet.of(EstadoContratacionDTO.CONFIRMADA,
            EstadoContratacionDTO.EN_CURSO, EstadoContratacionDTO.COMPLETADA, EstadoContratacionDTO.CANCELADA);
    private static final Set<EstadoContratacionDTO> REPROGRAMABLES = EnumSet.of(EstadoContratacionDTO.ACEPTADA,
            EstadoContratacionDTO.CONFIRMADA);

    private final Controladores app;
    private String usuario;
    private int trabajosEnCurso;
    private final List<JComponent> bloqueables = new ArrayList<>();

    // Cabecera y estado
    private final JComboBox<String> selectorUsuario = new JComboBox<>(USUARIOS.toArray(String[]::new));
    private final JLabel saldoCabecera = new JLabel();
    private final JLabel barraEstado = new JLabel(" ");
    private final JTabbedPane pestanias = new JTabbedPane();

    // Explorar
    private final JTextField textoBusqueda = new JTextField(22);
    private final DefaultTableModel modeloPublicaciones =
            modelo("Publicación", "Categoría", "Modalidad", "Precio", "Duración", "Proveedor", "Reputación");
    private final JTable tablaPublicaciones = tabla(modeloPublicaciones);
    private final JLabel tituloResultados = new JLabel(" ");
    private List<PublicacionDTO> publicaciones = List.of();
    private final JTextArea detallePublicacion = areaTexto(6);
    private final JComboBox<FranjaItem> franjas = new JComboBox<>();
    private final JTextField mensaje = new JTextField();
    private final JTextField objetivo = new JTextField();

    // Contrataciones
    private final DefaultTableModel modeloContrataciones =
            modelo("Publicación", "Con", "Precio", "Estado", "Solicitada");
    private final JTable tablaContrataciones = tabla(modeloContrataciones);
    private List<ContratacionDTO> contrataciones = List.of();
    private final Map<JButton, Set<EstadoContratacionDTO>> acciones = new LinkedHashMap<>();

    // Billetera
    private final JLabel saldo = new JLabel();
    private final DefaultTableModel modeloMovimientos = modelo("Fecha", "Tipo", "Monto");

    // Notificaciones
    private final DefaultTableModel modeloNotificaciones = modelo("", "Fecha", "Título", "Detalle");
    private final JLabel mocks = new JLabel(" ");

    // Perfil y publicaciones propias (4.2)
    private final JTextArea perfil = areaTexto(5);
    private final DefaultTableModel modeloPropias = modelo("Publicación", "Estado", "Precio", "Duración", "Franjas");
    private final JTable tablaPropias = tabla(modeloPropias);
    private List<PublicacionDTO> propias = List.of();
    private final Map<JButton, Set<EstadoPublicacion>> accionesPublicacion = new LinkedHashMap<>();
    /* acme-admin no tiene perfil: representa a una organizacion y publica desafios. */
    private boolean representaOrganizacion;
    /* El aviso de "sin perfil" se muestra al elegir el usuario, no en cada refresco. */
    private boolean avisarAlCargar;

    public SkillSwapFrame(Controladores app) {
        super("SkillSwap");
        this.app = app;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1200, 740);
        setMinimumSize(new Dimension(980, 620));
        setLocationRelativeTo(null);

        pestanias.setName("pestanias");
        pestanias.addTab("Explorar", panelExplorar());
        pestanias.addTab("Mis contrataciones", panelContrataciones());
        pestanias.addTab("Billetera", panelBilletera());
        pestanias.addTab("Notificaciones", panelNotificaciones());
        pestanias.addTab("Mi perfil", panelPerfil());
        pestanias.setBorder(BorderFactory.createEmptyBorder(10, 12, 0, 12));

        barraEstado.setName("barraEstado");
        barraEstado.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 14, 8, 14)));

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(cabecera(), BorderLayout.NORTH);
        raiz.add(pestanias, BorderLayout.CENTER);
        raiz.add(barraEstado, BorderLayout.SOUTH);
        setContentPane(raiz);

        selectorUsuario.addActionListener(e -> cambiarUsuario((String) selectorUsuario.getSelectedItem()));
        cambiarUsuario(USUARIOS.get(0));
        buscar();
    }

    // =====================================================================================
    // Armado de la ventana
    // =====================================================================================

    private JComponent cabecera() {
        JLabel titulo = new JLabel("SkillSwap");
        titulo.setForeground(Color.WHITE);
        titulo.setFont(titulo.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitulo = new JLabel("Mentorías, servicios y desafíos pagados con SkillCoins");
        subtitulo.setForeground(ACENTO_SUAVE);
        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        JLabel ingresar = new JLabel("Usuario (login simulado)");
        ingresar.setForeground(ACENTO_SUAVE);
        selectorUsuario.setName("usuario");
        saldoCabecera.setForeground(Color.WHITE);
        saldoCabecera.setFont(fuente(Font.BOLD, 15f));
        saldoCabecera.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
        JButton demo = boton("Demo guiada", this::demoGuiada, "demo");

        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        derecha.setOpaque(false);
        derecha.add(ingresar);
        derecha.add(selectorUsuario);
        derecha.add(saldoCabecera);
        derecha.add(demo);
        bloqueables.add(selectorUsuario);

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.setBackground(ACENTO);
        cabecera.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        cabecera.add(textos, BorderLayout.WEST);
        cabecera.add(derecha, BorderLayout.EAST);
        return cabecera;
    }

    private JComponent panelExplorar() {
        textoBusqueda.setName("textoBusqueda");
        textoBusqueda.addActionListener(e -> buscar());
        JPanel filtros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filtros.add(new JLabel("Buscar"));
        filtros.add(textoBusqueda);
        filtros.add(boton("Buscar", this::buscar, "buscar"));
        filtros.add(Box.createHorizontalStrut(16));
        filtros.add(new JLabel("Recomendadas:"));
        filtros.add(boton("Por mis habilidades", () -> recomendar(TipoRecomendacion.HABILIDADES), "recomendarHabilidades"));
        filtros.add(boton("Populares", () -> recomendar(TipoRecomendacion.POPULARIDAD_RECIENTE), "recomendarPopulares"));

        tablaPublicaciones.setName("tablaPublicaciones");
        anchos(tablaPublicaciones, 300, 150, 100, 80, 80, 140, 80);
        tablaPublicaciones.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarPublicacion();
            }
        });
        tituloResultados.setForeground(GRIS);
        JPanel resultados = new JPanel(new BorderLayout(0, 6));
        resultados.add(tituloResultados, BorderLayout.NORTH);
        resultados.add(new JScrollPane(tablaPublicaciones), BorderLayout.CENTER);

        franjas.setName("franja");
        mensaje.setName("mensaje");
        objetivo.setName("objetivo");
        JButton solicitar = boton("Solicitar", this::solicitar, "solicitar");
        solicitar.setFont(solicitar.getFont().deriveFont(Font.BOLD));

        JPanel formulario = new JPanel(new GridBagLayout());
        formulario.setBorder(seccion("Solicitar la publicación elegida"));
        int y = 0;
        filaCompleta(formulario, y++, new JScrollPane(detallePublicacion));
        fila(formulario, y++, "Franja", franjas);
        fila(formulario, y++, "Mensaje", mensaje);
        fila(formulario, y++, "Objetivo *", objetivo);
        filaCompleta(formulario, y++, solicitar);
        GridBagConstraints relleno = new GridBagConstraints();
        relleno.gridy = y;
        relleno.weighty = 1;
        formulario.add(Box.createGlue(), relleno);
        formulario.setPreferredSize(new Dimension(370, 0));

        JPanel panel = new JPanel(new BorderLayout(12, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
        panel.add(filtros, BorderLayout.NORTH);
        panel.add(resultados, BorderLayout.CENTER);
        panel.add(formulario, BorderLayout.EAST);
        return panel;
    }

    private JComponent panelContrataciones() {
        tablaContrataciones.setName("tablaContrataciones");
        anchos(tablaContrataciones, 320, 160, 90, 120, 140);
        tablaContrataciones.getColumnModel().getColumn(3).setCellRenderer(new EstadoRenderer());
        tablaContrataciones.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                habilitarAcciones();
            }
        });

        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 6));
        accion(botones, "Aceptar", "aceptar", EnumSet.of(EstadoContratacionDTO.PENDIENTE), c -> ejecutar(
                "Aceptando", u -> app.contrataciones().aceptar(u, c.getId()),
                "Solicitud aceptada: se reservó el lugar en la franja."));
        accion(botones, "Rechazar", "rechazar", EnumSet.of(EstadoContratacionDTO.PENDIENTE), c -> ejecutar(
                "Rechazando", u -> app.contrataciones().rechazar(u, c.getId()), "Solicitud rechazada."));
        accion(botones, "Pagar con SkillCoins", "pagar", EnumSet.of(EstadoContratacionDTO.ACEPTADA), c -> ejecutar(
                "Pagando", u -> app.contrataciones().pagar(u, c.getId()),
                "Pago confirmado: " + c.getPrecio() + " SkillCoins."));
        accion(botones, "Iniciar sesión", "iniciar", EnumSet.of(EstadoContratacionDTO.CONFIRMADA), c -> ejecutar(
                "Iniciando", u -> app.contrataciones().iniciar(u, c.getId()), "Sesión iniciada."));
        accion(botones, "Completar", "completar", EnumSet.of(EstadoContratacionDTO.EN_CURSO), c -> ejecutar(
                "Completando", u -> app.contrataciones().completar(u, c.getId()),
                "Sesión completada: ya se puede calificar."));
        accion(botones, "Cancelar…", "cancelar", REPROGRAMABLES, this::cancelar);
        botones.add(new JSeparator());
        accion(botones, "Proponer reprogramación…", "proponerReprogramacion", REPROGRAMABLES,
                this::proponerReprogramacion);
        accion(botones, "Aceptar reprogramación", "aceptarReprogramacion", REPROGRAMABLES, c -> ejecutar(
                "Reprogramando", u -> app.contrataciones().aceptarReprogramacion(u, c.getId()),
                "Reprogramación aplicada."));
        accion(botones, "Rechazar reprogramación", "rechazarReprogramacion", REPROGRAMABLES, c -> ejecutar(
                "Rechazando la propuesta", u -> app.contrataciones().rechazarReprogramacion(u, c.getId()),
                "Propuesta de reprogramación descartada."));
        botones.add(new JSeparator());
        accion(botones, "Calificar…", "calificar", EnumSet.of(EstadoContratacionDTO.COMPLETADA), this::calificar);
        accion(botones, "Ver historial", "historial", EnumSet.allOf(EstadoContratacionDTO.class), this::historial);
        accion(botones, "Ver comprobante", "comprobante", PAGADAS, this::comprobante);

        JLabel ayuda = new JLabel("<html>Los botones se habilitan según el estado (tabla 4.5). "
                + "El dominio valida además quién puede hacer cada acción.</html>");
        ayuda.setForeground(GRIS);
        JPanel lateral = new JPanel(new BorderLayout(0, 8));
        lateral.setBorder(seccion("Acciones"));
        lateral.add(botones, BorderLayout.NORTH);
        lateral.add(ayuda, BorderLayout.SOUTH);
        lateral.setPreferredSize(new Dimension(250, 0));

        JPanel panel = new JPanel(new BorderLayout(12, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
        panel.add(new JScrollPane(tablaContrataciones), BorderLayout.CENTER);
        panel.add(lateral, BorderLayout.EAST);
        return panel;
    }

    private JComponent panelBilletera() {
        saldo.setName("saldo");
        saldo.setFont(fuente(Font.BOLD, 30f));
        saldo.setForeground(ACENTO);
        JLabel nota = new JLabel("Carga sin pago real: existe solo para la demo.");
        nota.setForeground(GRIS);
        JPanel cabecera = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        cabecera.add(saldo);
        cabecera.add(boton("Cargar SkillCoins…", this::cargarSkillCoins, "cargar"));
        cabecera.add(nota);

        JTable movimientos = tabla(modeloMovimientos);
        movimientos.setName("tablaMovimientos");
        anchos(movimientos, 160, 260, 120);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(new JScrollPane(movimientos), BorderLayout.CENTER);
        return panel;
    }

    private JComponent panelNotificaciones() {
        JTable notificaciones = tabla(modeloNotificaciones);
        notificaciones.setName("tablaNotificaciones");
        anchos(notificaciones, 24, 130, 260, 520);
        mocks.setForeground(GRIS);
        JPanel arriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        arriba.add(boton("Marcar todas como leídas", () -> ejecutar("Marcando como leídas",
                u -> app.notificaciones().marcarTodasLeidas(u), "Notificaciones marcadas como leídas."), "marcarLeidas"));
        arriba.add(mocks);
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
        panel.add(arriba, BorderLayout.NORTH);
        panel.add(new JScrollPane(notificaciones), BorderLayout.CENTER);
        return panel;
    }

    private JComponent panelPerfil() {
        perfil.setName("perfil");
        tablaPropias.setName("tablaPropias");
        anchos(tablaPropias, 360, 110, 90, 90, 70);
        tablaPropias.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                habilitarAccionesPublicacion();
            }
        });

        JPanel botones = new JPanel(new GridLayout(0, 1, 0, 6));
        JButton nueva = boton("Nueva publicación…", this::nuevaPublicacion, "nuevaPublicacion");
        nueva.setFont(nueva.getFont().deriveFont(Font.BOLD));
        botones.add(nueva);
        botones.add(new JSeparator());
        accionPublicacion(botones, "Agregar franja…", "agregarFranja", EnumSet.of(EstadoPublicacion.BORRADOR),
                this::agregarFranja);
        accionPublicacion(botones, "Publicar", "publicar",
                EnumSet.of(EstadoPublicacion.BORRADOR, EstadoPublicacion.PAUSADA),
                p -> cambiarEstadoPublicacion(p, EstadoPublicacion.PUBLICADA,
                        "Publicada: ya aparece en Explorar y admite solicitudes."));
        accionPublicacion(botones, "Pausar", "pausar", EnumSet.of(EstadoPublicacion.PUBLICADA),
                p -> cambiarEstadoPublicacion(p, EstadoPublicacion.PAUSADA,
                        "Pausada: no aparece en Explorar ni admite solicitudes."));
        accionPublicacion(botones, "Cerrar", "cerrar",
                EnumSet.of(EstadoPublicacion.PUBLICADA, EstadoPublicacion.PAUSADA),
                p -> cambiarEstadoPublicacion(p, EstadoPublicacion.CERRADA,
                        "Cerrada: no recibe más solicitudes."));
        accionPublicacion(botones, "Cancelar…", "cancelarPublicacion",
                EnumSet.of(EstadoPublicacion.BORRADOR, EstadoPublicacion.PUBLICADA, EstadoPublicacion.PAUSADA),
                this::cancelarPublicacion);

        JLabel ayuda = new JLabel("<html>Ciclo: borrador → (franjas) → publicada ⇄ pausada → cerrada. "
                + "Las franjas se agregan mientras es borrador.</html>");
        ayuda.setForeground(GRIS);
        JPanel lateral = new JPanel(new BorderLayout(0, 8));
        lateral.setBorder(seccion("Publicaciones"));
        lateral.add(botones, BorderLayout.NORTH);
        lateral.add(ayuda, BorderLayout.SOUTH);
        lateral.setPreferredSize(new Dimension(250, 0));

        JPanel publicacionesPropias = new JPanel(new BorderLayout(12, 0));
        JPanel tablaConTitulo = new JPanel(new BorderLayout());
        tablaConTitulo.setBorder(seccion("Mis publicaciones"));
        tablaConTitulo.add(new JScrollPane(tablaPropias), BorderLayout.CENTER);
        publicacionesPropias.add(tablaConTitulo, BorderLayout.CENTER);
        publicacionesPropias.add(lateral, BorderLayout.EAST);

        JScrollPane datos = new JScrollPane(perfil);
        datos.setBorder(seccion("Perfil público"));
        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 4, 8, 4));
        panel.add(datos, BorderLayout.NORTH);
        panel.add(publicacionesPropias, BorderLayout.CENTER);
        return panel;
    }

    // =====================================================================================
    // Casos de uso
    // =====================================================================================

    private void cambiarUsuario(String nuevo) {
        usuario = nuevo;
        avisarAlCargar = true;
        tablaContrataciones.clearSelection();
        ok("Sesión iniciada como " + nuevo + ".");
        refrescar();
    }

    private void buscar() {
        String texto = textoBusqueda.getText();
        trabajo("Buscando publicaciones",
                () -> app.busqueda().buscar(new FiltrosBusqueda(texto, null, null, null, null, null, null, null))
                        .getElementos(),
                lista -> mostrarPublicaciones(lista, texto.isBlank()
                        ? "Todas las publicaciones" : "Resultados para \"" + texto.trim() + "\""));
    }

    private void recomendar(TipoRecomendacion tipo) {
        String u = usuario;
        String descripcion = tipo == TipoRecomendacion.HABILIDADES
                ? "Recomendadas para " + u + " por sus habilidades" : "Populares de los últimos 30 días";
        trabajo("Buscando recomendaciones", () -> app.recomendaciones().recomendar(u, tipo, 0, 20).getElementos(),
                lista -> mostrarPublicaciones(lista, descripcion));
    }

    private void solicitar() {
        PublicacionDTO publicacion = publicacionSeleccionada();
        FranjaItem franja = (FranjaItem) franjas.getSelectedItem();
        if (publicacion == null || franja == null) {
            error("Elegí una publicación y una franja horaria.");
            return;
        }
        String textoMensaje = mensaje.getText();
        String textoObjetivo = objetivo.getText();
        ejecutar("Enviando la solicitud",
                u -> app.contrataciones().solicitar(u, publicacion.getId(), textoMensaje, franja.franja().getId(),
                        textoObjetivo),
                "Solicitud enviada: queda PENDIENTE hasta que el proveedor la acepte (ver \"Mis contrataciones\").");
    }

    private void cancelar(ContratacionDTO c) {
        String motivo = JOptionPane.showInputDialog(this, "¿Por qué cancelás \"" + c.getTitulo() + "\"?",
                "Cancelar contratación", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        ejecutar("Cancelando", u -> app.contrataciones().cancelar(u, c.getId(), motivo),
                "Contratación cancelada. Si estaba pagada, el reintegro depende de la anticipación.");
    }

    private void proponerReprogramacion(ContratacionDTO c) {
        trabajo("Buscando las franjas de la publicación", () -> app.busqueda()
                        .buscar(new FiltrosBusqueda(c.getTitulo(), null, null, null, null, null, null, null))
                        .getElementos().stream()
                        .filter(p -> p.getTitulo().equals(c.getTitulo()))
                        .findFirst()
                        .map(PublicacionDTO::getFranjas)
                        .orElse(List.of()),
                lista -> {
                    if (lista.isEmpty()) {
                        error("La publicación ya no está visible: no se pueden elegir franjas.");
                        return;
                    }
                    JComboBox<FranjaItem> opciones = new JComboBox<>(lista.stream().map(FranjaItem::new)
                            .toArray(FranjaItem[]::new));
                    if (confirmar("Proponer reprogramación", "Nueva franja para \"" + c.getTitulo() + "\"", opciones)) {
                        UUID franjaId = ((FranjaItem) opciones.getSelectedItem()).franja().getId();
                        ejecutar("Proponiendo la reprogramación",
                                u -> app.contrataciones().proponerReprogramacion(u, c.getId(), franjaId),
                                "Propuesta enviada: la otra parte tiene que aceptarla.");
                    }
                });
    }

    private void calificar(ContratacionDTO c) {
        JComboBox<Integer> puntaje = new JComboBox<>(new Integer[]{5, 4, 3, 2, 1});
        if (confirmar("Calificar", "Puntaje para " + c.getContraparte() + " (1 a 5)", puntaje)) {
            int valor = (Integer) puntaje.getSelectedItem();
            ejecutar("Calificando",
                    u -> app.calificaciones().calificar(u, c.getId(), c.getContraparteUserId(), valor),
                    "Calificación registrada: actualiza la reputación de " + c.getContraparte() + ".");
        }
    }

    private void historial(ContratacionDTO c) {
        String u = usuario;
        trabajo("Consultando el historial", () -> app.contrataciones().consultarHistorial(u, c.getId()), registros -> {
            DefaultTableModel modelo = modelo("Fecha", "De", "A", "Actor", "Motivo");
            for (RegistroHistorialDTO r : registros) {
                modelo.addRow(new Object[]{r.getFecha() == null ? "" : FECHA_LOCAL.format(r.getFecha()),
                        r.getOrigen(), r.getDestino(), r.getActor(), r.getMotivo()});
            }
            JScrollPane tabla = new JScrollPane(tabla(modelo));
            tabla.setPreferredSize(new Dimension(760, 220));
            JOptionPane.showMessageDialog(this, tabla, "Historial de \"" + c.getTitulo() + "\"",
                    JOptionPane.PLAIN_MESSAGE);
            ok("Historial consultado: " + registros.size() + " transiciones.");
        });
    }

    private void comprobante(ContratacionDTO c) {
        String u = usuario;
        trabajo("Consultando el comprobante", () -> app.contrataciones().consultarComprobante(u, c.getId()),
                (ComprobantePagoDTO comprobante) -> {
                    String texto = "Publicación: " + c.getTitulo()
                            + "\nTotal pagado: " + comprobante.getTotal() + " SkillCoins"
                            + "\nFecha: " + FECHA.format(comprobante.getFecha())
                            + "\nComprobante N.º " + comprobante.getPagoId();
                    JOptionPane.showMessageDialog(this, texto, "Comprobante de pago",
                            JOptionPane.INFORMATION_MESSAGE);
                    ok("Comprobante consultado.");
                });
    }

    // ---------- publicaciones propias (4.2) ----------

    /* Primero trae las categorias; despues pide los datos. Opcional: primera franja y publicar en el mismo paso. */
    private void nuevaPublicacion() {
        trabajo("Cargando categorías", () -> app.busqueda().listarCategorias(), categorias -> {
            JComboBox<TipoPublicacion> tipo = new JComboBox<>(TipoPublicacion.values());
            tipo.setSelectedItem(representaOrganizacion ? TipoPublicacion.DESAFIO : TipoPublicacion.MENTORIA);
            JTextField titulo = new JTextField(28);
            JTextArea descripcion = new JTextArea(3, 28);
            descripcion.setLineWrap(true);
            descripcion.setWrapStyleWord(true);
            descripcion.setFont(fuente(Font.PLAIN, 13f));
            JComboBox<CategoriaItem> categoria = new JComboBox<>(categorias.stream().map(CategoriaItem::new)
                    .toArray(CategoriaItem[]::new));
            JComboBox<Modalidad> modalidad = new JComboBox<>(Modalidad.values());
            JSpinner duracion = new JSpinner(new SpinnerNumberModel(60, 15, 960, 15));
            JSpinner cupo = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
            JSpinner precio = new JSpinner(new SpinnerNumberModel(100, 0, 100_000, 10));
            JTextField etiquetas = new JTextField();
            JCheckBox conFranja = new JCheckBox("Agregar la primera franja", true);
            JSpinner inicio = selectorFechaHora(proximaSemana());
            JCheckBox publicar = new JCheckBox("Publicar ahora (si no, queda como borrador)", true);
            conFranja.addActionListener(e -> {
                inicio.setEnabled(conFranja.isSelected());
                publicar.setEnabled(conFranja.isSelected());
                publicar.setSelected(conFranja.isSelected() && publicar.isSelected());
            });

            JPanel formulario = new JPanel(new GridBagLayout());
            int y = 0;
            fila(formulario, y++, "Tipo", tipo);
            fila(formulario, y++, "Título *", titulo);
            fila(formulario, y++, "Descripción", new JScrollPane(descripcion));
            fila(formulario, y++, "Categoría", categoria);
            fila(formulario, y++, "Modalidad", modalidad);
            fila(formulario, y++, "Duración (min)", duracion);
            fila(formulario, y++, "Cupo por franja", cupo);
            fila(formulario, y++, "Precio (SkillCoins)", precio);
            fila(formulario, y++, "Etiquetas", etiquetas);
            filaCompleta(formulario, y++, new JSeparator());
            filaCompleta(formulario, y++, conFranja);
            fila(formulario, y++, "Inicio", inicio);
            filaCompleta(formulario, y, publicar);

            if (JOptionPane.showConfirmDialog(this, formulario, "Nueva publicación", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE) != JOptionPane.OK_OPTION) {
                return;
            }
            CategoriaItem elegida = (CategoriaItem) categoria.getSelectedItem();
            int minutos = (Integer) duracion.getValue();
            DatosPublicacionDTO datos = new DatosPublicacionDTO(titulo.getText(), descripcion.getText(),
                    elegida == null ? null : elegida.categoria().id(), (Modalidad) modalidad.getSelectedItem(),
                    minutos, (Integer) cupo.getValue(), (Integer) precio.getValue(), etiquetas(etiquetas.getText()),
                    (TipoPublicacion) tipo.getSelectedItem(), null);
            boolean agregarFranja = conFranja.isSelected();
            boolean publicarAhora = publicar.isSelected() && agregarFranja;
            Instant desde = fechaHora(inicio);
            ejecutar("Creando la publicación", u -> {
                PublicacionDTO creada = app.publicaciones().crearBorrador(u, datos);
                if (agregarFranja) {
                    app.publicaciones().agregarFranja(u, creada.getId(), desde, desde.plus(Duration.ofMinutes(minutos)));
                }
                if (publicarAhora) {
                    app.publicaciones().cambiarEstado(u, creada.getId().toString(), EstadoPublicacion.PUBLICADA);
                }
            }, publicarAhora ? "Publicación creada y publicada: ya aparece en Explorar."
                    : "Borrador creado: agregale franjas y publicalo desde \"Mi perfil\".");
        });
    }

    private void agregarFranja(PublicacionDTO p) {
        JSpinner inicio = selectorFechaHora(proximaSemana());
        if (!confirmar("Agregar franja", "Inicio de la franja de \"" + p.getTitulo() + "\" (dura "
                + p.getDuracionMinutos() + " min)", inicio)) {
            return;
        }
        Instant desde = fechaHora(inicio);
        Instant hasta = desde.plus(Duration.ofMinutes(p.getDuracionMinutos()));
        ejecutar("Agregando la franja", u -> app.publicaciones().agregarFranja(u, p.getId(), desde, hasta),
                "Franja agregada: " + FECHA.format(desde) + " a " + HORA.format(hasta) + ".");
    }

    private void cambiarEstadoPublicacion(PublicacionDTO p, EstadoPublicacion estado, String exito) {
        ejecutar("Actualizando la publicación",
                u -> app.publicaciones().cambiarEstado(u, p.getId().toString(), estado), exito);
    }

    private void cancelarPublicacion(PublicacionDTO p) {
        String motivo = JOptionPane.showInputDialog(this, "¿Por qué cancelás \"" + p.getTitulo() + "\"?",
                "Cancelar publicación", JOptionPane.QUESTION_MESSAGE);
        if (motivo == null) {
            return;
        }
        ejecutar("Cancelando la publicación", u -> app.publicaciones().cancelar(u, p.getId(), motivo),
                "Publicación cancelada: ya no se puede aceptar ni pagar nada sobre ella.");
    }

    private void cargarSkillCoins() {
        String importe = JOptionPane.showInputDialog(this, "¿Cuántos SkillCoins cargás? (solo demo)", "100");
        if (importe == null) {
            return;
        }
        int valor;
        try {
            valor = Integer.parseInt(importe.trim());
        } catch (NumberFormatException e) {
            error("El importe tiene que ser un número entero.");
            return;
        }
        ejecutar("Cargando SkillCoins", u -> app.billetera().acreditarDemo(u, valor),
                "Se cargaron " + valor + " SkillCoins.");
    }

    private void demoGuiada() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "La demo recorre el flujo completo con bruno, ana y diego y modifica los datos\n"
                        + "(cada corrida ocupa un lugar de la franja). ¿Continuar?",
                "Demo guiada", JOptionPane.OK_CANCEL_OPTION);
        if (respuesta != JOptionPane.OK_OPTION) {
            return;
        }
        trabajo("Corriendo la demo guiada", () -> {
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            new DemoGuiada(app, new PrintStream(salida, true, StandardCharsets.UTF_8)).ejecutar();
            return salida.toString(StandardCharsets.UTF_8);
        }, texto -> {
            JTextArea area = areaTexto(24);
            area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
            area.setText(texto);
            area.setCaretPosition(0);
            JScrollPane scroll = new JScrollPane(area);
            scroll.setPreferredSize(new Dimension(780, 480));
            JOptionPane.showMessageDialog(this, scroll, "Demo guiada", JOptionPane.PLAIN_MESSAGE);
            ok("Demo guiada terminada.");
            refrescar();
        });
    }

    // =====================================================================================
    // Datos del usuario
    // =====================================================================================

    /* Todo lo que depende del usuario se carga junto; si una parte falla (ej. acme-admin no tiene perfil), sigue. */
    private record DatosUsuario(List<ContratacionDTO> contrataciones, int saldo, List<MovimientoDTO> movimientos,
                                List<NotificacionDTO> notificaciones, PerfilPublicoDTO perfil,
                                List<PublicacionDTO> propias, List<String> avisos) {
    }

    private void refrescar() {
        String u = usuario;
        trabajo(null, () -> cargar(u), datos -> {
            if (u.equals(usuario)) {
                mostrar(datos);
            }
        });
    }

    private DatosUsuario cargar(String u) {
        List<String> avisos = new ArrayList<>();
        return new DatosUsuario(
                intentar(() -> app.contrataciones().listarContrataciones(u), List.of(), avisos),
                intentar(() -> app.billetera().consultarSaldo(u), 0, avisos),
                intentar(() -> app.billetera().listarMovimientos(u), List.of(), avisos),
                intentar(() -> app.notificaciones().listar(u, 0, 50).getElementos(), List.of(), avisos),
                intentar(() -> app.perfiles().verPerfil(u), null, avisos),
                intentar(() -> app.publicaciones().listarPropias(u), List.of(), avisos),
                avisos);
    }

    private static <T> T intentar(Supplier<T> consulta, T porDefecto, List<String> avisos) {
        try {
            return consulta.get();
        } catch (ErrorAplicacion e) {
            if (!avisos.contains(e.getMessage())) {
                avisos.add(e.getMessage());
            }
            return porDefecto;
        }
    }

    private void mostrar(DatosUsuario datos) {
        UUID seleccionada = contratacionSeleccionada() == null ? null : contratacionSeleccionada().getId();
        contrataciones = datos.contrataciones();
        modeloContrataciones.setRowCount(0);
        for (ContratacionDTO c : contrataciones) {
            modeloContrataciones.addRow(new Object[]{c.getTitulo(), c.getContraparte(), c.getPrecio() + " SC",
                    c.getEstado(), c.getFecha() == null ? "" : FECHA_LOCAL.format(c.getFecha())});
        }
        for (int i = 0; i < contrataciones.size(); i++) {
            if (contrataciones.get(i).getId().equals(seleccionada)) {
                tablaContrataciones.setRowSelectionInterval(i, i);
            }
        }
        habilitarAcciones();
        pestanias.setTitleAt(1, "Mis contrataciones (" + contrataciones.size() + ")");

        saldo.setText(datos.saldo() + " SkillCoins");
        saldoCabecera.setText("Saldo: " + datos.saldo() + " SC");
        modeloMovimientos.setRowCount(0);
        for (MovimientoDTO m : datos.movimientos()) {
            modeloMovimientos.addRow(new Object[]{FECHA.format(m.getFecha()), m.getTipo(), m.getMonto()});
        }

        modeloNotificaciones.setRowCount(0);
        long sinLeer = 0;
        for (NotificacionDTO n : datos.notificaciones()) {
            sinLeer += n.isLeida() ? 0 : 1;
            modeloNotificaciones.addRow(new Object[]{n.isLeida() ? "" : "●", FECHA.format(n.getFecha()),
                    n.getTitulo(), n.getDetalle()});
        }
        pestanias.setTitleAt(3, sinLeer > 0 ? "Notificaciones (" + sinLeer + ")" : "Notificaciones");
        mocks.setText("Enviados en esta ejecución por los adaptadores mock: " + app.emailsEnviados()
                + " emails y " + app.pushEnviados() + " push.");

        PerfilPublicoDTO p = datos.perfil();
        representaOrganizacion = p == null;
        perfil.setText(p == null
                ? usuario + " no tiene perfil: representa a una organización (publica desafíos)."
                : p.getNombreVisible() + "  (" + p.getUserId() + ")  ·  " + p.getRol() + "\n"
                + p.getBiografia() + "\n\n"
                + String.format("Reputación: %.1f con %d reseñas  ·  Proyectos completados: %d%n",
                p.getReputacion(), p.getResenas(), p.getProyectosCompletados())
                + "Habilidades: " + String.join(", ", p.getHabilidades()));
        perfil.setCaretPosition(0);
        UUID propiaSeleccionada = publicacionPropiaSeleccionada() == null ? null
                : publicacionPropiaSeleccionada().getId();
        propias = datos.propias();
        modeloPropias.setRowCount(0);
        for (PublicacionDTO pub : propias) {
            modeloPropias.addRow(new Object[]{pub.getTitulo(), pub.getEstado(), pub.getPrecio() + " SC",
                    pub.getDuracionMinutos() + " min", pub.getFranjas().size()});
        }
        for (int i = 0; i < propias.size(); i++) {
            if (propias.get(i).getId().equals(propiaSeleccionada)) {
                tablaPropias.setRowSelectionInterval(i, i);
            }
        }
        habilitarAccionesPublicacion();

        if (avisarAlCargar && p == null) {
            info(usuario + " representa a una organización: no tiene perfil público, pero puede publicar desafíos.");
        }
        avisarAlCargar = false;
    }

    private void mostrarPublicaciones(List<PublicacionDTO> lista, String descripcion) {
        mostrarPublicaciones(lista, descripcion, true);
    }

    /* avisar=false: refresco silencioso, que ademas mantiene la publicacion elegida si sigue en la lista. */
    private void mostrarPublicaciones(List<PublicacionDTO> lista, String descripcion, boolean avisar) {
        UUID elegida = publicacionSeleccionada() == null ? null : publicacionSeleccionada().getId();
        publicaciones = lista;
        modeloPublicaciones.setRowCount(0);
        for (PublicacionDTO p : lista) {
            modeloPublicaciones.addRow(new Object[]{p.getTitulo(), p.getCategoria(), p.getModalidad(),
                    p.getPrecio() + " SC", p.getDuracionMinutos() + " min", p.getProveedor(),
                    String.format("%.1f", p.getReputacion())});
        }
        tituloResultados.setText(descripcion + " (" + lista.size() + ")");
        int fila = 0;
        for (int i = 0; i < lista.size(); i++) {
            if (!avisar && lista.get(i).getId().equals(elegida)) {
                fila = i;
            }
        }
        if (lista.isEmpty()) {
            mostrarPublicacion();
        } else {
            tablaPublicaciones.setRowSelectionInterval(fila, fila);
        }
        if (avisar) {
            ok(descripcion + ": " + lista.size() + " publicaciones.");
        }
    }

    private void mostrarPublicacion() {
        PublicacionDTO p = publicacionSeleccionada();
        franjas.removeAllItems();
        if (p == null) {
            detallePublicacion.setText("Elegí una publicación de la tabla.");
            return;
        }
        detallePublicacion.setText(p.getTitulo() + "\n\n" + p.getDescripcion()
                + (p.getEtiquetas().isEmpty() ? "" : "\n\nEtiquetas: " + String.join(", ", p.getEtiquetas())));
        detallePublicacion.setCaretPosition(0);
        p.getFranjas().forEach(f -> franjas.addItem(new FranjaItem(f)));
    }

    private void habilitarAcciones() {
        ContratacionDTO c = contratacionSeleccionada();
        boolean libre = trabajosEnCurso == 0;
        acciones.forEach((boton, estados) -> boton.setEnabled(libre && c != null && estados.contains(c.getEstado())));
    }

    private void habilitarAccionesPublicacion() {
        PublicacionDTO p = publicacionPropiaSeleccionada();
        boolean libre = trabajosEnCurso == 0;
        accionesPublicacion.forEach((boton, estados) ->
                boton.setEnabled(libre && p != null && estados.contains(p.getEstado())));
    }

    private PublicacionDTO publicacionPropiaSeleccionada() {
        int fila = tablaPropias.getSelectedRow();
        return fila < 0 || fila >= propias.size() ? null : propias.get(fila);
    }

    private PublicacionDTO publicacionSeleccionada() {
        int fila = tablaPublicaciones.getSelectedRow();
        return fila < 0 || fila >= publicaciones.size() ? null : publicaciones.get(fila);
    }

    private ContratacionDTO contratacionSeleccionada() {
        int fila = tablaContrataciones.getSelectedRow();
        return fila < 0 || fila >= contrataciones.size() ? null : contrataciones.get(fila);
    }

    // =====================================================================================
    // Ejecucion en segundo plano y mensajes
    // =====================================================================================

    /* Accion que cambia datos: corre el caso de uso con el usuario actual y despues refresca todo. */
    private void ejecutar(String descripcion, Consumer<String> casoDeUso, String exito) {
        String u = usuario;
        trabajo(descripcion, () -> {
            casoDeUso.accept(u);
            return Boolean.TRUE;
        }, listo -> {
            ok(exito);
            refrescar();
            actualizarExplorar();
        });
    }

    /* Repite la ultima busqueda sin tocar la barra de estado (ej. despues de publicar o de aceptar un cupo). */
    private void actualizarExplorar() {
        String texto = textoBusqueda.getText();
        trabajo(null, () -> app.busqueda()
                        .buscar(new FiltrosBusqueda(texto, null, null, null, null, null, null, null)).getElementos(),
                lista -> mostrarPublicaciones(lista, texto.isBlank()
                        ? "Todas las publicaciones" : "Resultados para \"" + texto.trim() + "\"", false));
    }

    /* La llamada al controller corre fuera del hilo de Swing; el resultado se muestra en el hilo de Swing. */
    private <T> void trabajo(String descripcion, Callable<T> tarea, Consumer<T> alTerminar) {
        trabajosEnCurso++;
        actualizarBloqueo();
        if (descripcion != null) {
            info(descripcion + "…");
        }
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return tarea.call();
            }

            @Override
            protected void done() {
                trabajosEnCurso--;
                actualizarBloqueo();
                try {
                    alTerminar.accept(get());
                } catch (ExecutionException e) {
                    mostrarFallo(e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }.execute();
    }

    private void actualizarBloqueo() {
        boolean libre = trabajosEnCurso == 0;
        bloqueables.forEach(c -> c.setEnabled(libre));
        habilitarAcciones();
        habilitarAccionesPublicacion();
        setCursor(libre ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
    }

    private void mostrarFallo(Throwable causa) {
        String texto;
        if (causa instanceof ErrorAplicacion e) {
            texto = switch (e.getTipo()) {
                case DATOS_INVALIDOS -> "Datos inválidos: ";
                case SIN_PERMISO -> "Sin permiso: ";
                case ESTADO_INVALIDO -> "No se puede ahora: ";
                case CONFLICTO -> "Conflicto: ";
                case ERROR_INTERNO -> "Error interno: ";
            } + e.getMessage();
        } else {
            texto = "Error inesperado: " + causa;
        }
        error(texto);
        JOptionPane.showMessageDialog(this, texto, "No se pudo completar la operación", JOptionPane.WARNING_MESSAGE);
    }

    private void ok(String texto) {
        barraEstado.setForeground(VERDE);
        barraEstado.setText("✔  " + texto);
    }

    private void info(String texto) {
        barraEstado.setForeground(GRIS);
        barraEstado.setText(texto);
    }

    private void error(String texto) {
        barraEstado.setForeground(ROJO);
        barraEstado.setText("✖  " + texto);
    }

    private boolean confirmar(String titulo, String pregunta, JComponent campo) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel(pregunta), BorderLayout.NORTH);
        panel.add(campo, BorderLayout.CENTER);
        return JOptionPane.showConfirmDialog(this, panel, titulo, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    // =====================================================================================
    // Componentes
    // =====================================================================================

    private JButton boton(String texto, Runnable accion, String nombre) {
        JButton boton = new JButton(texto);
        boton.setName(nombre);
        boton.addActionListener(e -> accion.run());
        bloqueables.add(boton);
        return boton;
    }

    private void accion(JPanel panel, String texto, String nombre, Set<EstadoContratacionDTO> estados,
                        Consumer<ContratacionDTO> accion) {
        JButton boton = new JButton(texto);
        boton.setName(nombre);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setEnabled(false);
        boton.addActionListener(e -> {
            ContratacionDTO c = contratacionSeleccionada();
            if (c != null) {
                accion.accept(c);
            }
        });
        acciones.put(boton, estados);
        panel.add(boton);
    }

    private void accionPublicacion(JPanel panel, String texto, String nombre, Set<EstadoPublicacion> estados,
                                   Consumer<PublicacionDTO> accion) {
        JButton boton = new JButton(texto);
        boton.setName(nombre);
        boton.setHorizontalAlignment(SwingConstants.LEFT);
        boton.setEnabled(false);
        boton.addActionListener(e -> {
            PublicacionDTO p = publicacionPropiaSeleccionada();
            if (p != null) {
                accion.accept(p);
            }
        });
        accionesPublicacion.put(boton, estados);
        panel.add(boton);
    }

    private static JSpinner selectorFechaHora(Instant inicial) {
        JSpinner selector = new JSpinner(new SpinnerDateModel(Date.from(inicial), null, null, Calendar.MINUTE));
        selector.setEditor(new JSpinner.DateEditor(selector, "dd/MM/yyyy HH:mm"));
        return selector;
    }

    private static Instant fechaHora(JSpinner selector) {
        return ((Date) selector.getValue()).toInstant();
    }

    /* Valor inicial de los selectores: dentro de una semana a las 18 h (hora local). */
    private static Instant proximaSemana() {
        return LocalDate.now().plusDays(7).atTime(18, 0).atZone(ZoneId.systemDefault()).toInstant();
    }

    /* "java, backend ,  POO" -> {java, backend, POO} */
    private static Set<String> etiquetas(String texto) {
        Set<String> resultado = new LinkedHashSet<>();
        for (String etiqueta : texto.split(",")) {
            if (!etiqueta.isBlank()) {
                resultado.add(etiqueta.trim());
            }
        }
        return resultado;
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }

    private static JTable tabla(DefaultTableModel modelo) {
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(26);
        tabla.setFillsViewportHeight(true);
        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabla.getTableHeader().setReorderingAllowed(false);
        tabla.setShowVerticalLines(false);
        tabla.setGridColor(new Color(226, 232, 240));
        return tabla;
    }

    /* La fuente de las etiquetas del look and feel (en Windows, Segoe UI), en el estilo y tamanio pedidos. */
    private static Font fuente(int estilo, float tamanio) {
        Font base = UIManager.getFont("Label.font");
        return (base != null ? base : new Font(Font.SANS_SERIF, Font.PLAIN, 12)).deriveFont(estilo, tamanio);
    }

    private static void anchos(JTable tabla, int... anchos) {
        for (int i = 0; i < anchos.length; i++) {
            tabla.getColumnModel().getColumn(i).setPreferredWidth(anchos[i]);
        }
    }

    private static JTextArea areaTexto(int filas) {
        JTextArea area = new JTextArea(filas, 20);
        area.setFont(fuente(Font.PLAIN, 13f));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        return area;
    }

    private static javax.swing.border.Border seccion(String titulo) {
        return BorderFactory.createCompoundBorder(BorderFactory.createTitledBorder(titulo),
                BorderFactory.createEmptyBorder(6, 8, 8, 8));
    }

    private static void fila(JPanel panel, int y, String etiqueta, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(4, 0, 4, 8);
        panel.add(new JLabel(etiqueta), c);
        c.gridx = 1;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 4, 0);
        panel.add(campo, c);
    }

    private static void filaCompleta(JPanel panel, int y, JComponent campo) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridy = y;
        c.gridx = 0;
        c.gridwidth = 2;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 0, 6, 0);
        panel.add(campo, c);
    }

    /* Como se muestra una categoria en el combo. */
    private record CategoriaItem(CategoriaDTO categoria) {
        @Override
        public String toString() {
            return categoria.nombre();
        }
    }

    /* Como se muestra una franja en los combos. */
    private record FranjaItem(FranjaDTO franja) {
        @Override
        public String toString() {
            return FECHA.format(franja.getInicio()) + " a " + HORA.format(franja.getFin())
                    + "  (" + franja.getLugaresLibres() + " libres)";
        }
    }

    /* Colorea la columna de estado de las contrataciones. */
    private static final class EstadoRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable tabla, Object valor, boolean seleccionada,
                                                       boolean foco, int fila, int columna) {
            Component c = super.getTableCellRendererComponent(tabla, valor, seleccionada, foco, fila, columna);
            setFont(getFont().deriveFont(Font.BOLD));
            if (!seleccionada && valor instanceof EstadoContratacionDTO estado) {
                setForeground(switch (estado) {
                    case PENDIENTE -> new Color(180, 83, 9);
                    case ACEPTADA -> ACENTO;
                    case CONFIRMADA, EN_CURSO -> new Color(109, 40, 217);
                    case COMPLETADA -> VERDE;
                    case RECHAZADA, CANCELADA -> GRIS;
                });
            }
            return c;
        }
    }
}
