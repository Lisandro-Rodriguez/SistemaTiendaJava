package com.tienda.vista;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import com.formdev.flatlaf.*;
import com.tienda.Modelo.Producto;
import com.tienda.db.CatalogoDAO;
import com.tienda.db.ProductoDAO;

@SuppressWarnings({"java:S2221", "java:S1181"})
public class VentanaProducto extends JFrame {

    private JTabbedPane pestanas;

    // Pestaña Registro
    private JTextField txtCodigoBarras, txtNombre, txtCosto, txtStock, txtTipo, txtMarca;
    private JComboBox<String> comboMargen;

    // Pestaña Inventario
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorterInventario;

    // Pestaña Ventas
    private DefaultTableModel modeloVenta;
    private JTable tablaVenta;
    private JLabel lblTotal;
    private double totalVenta = 0;
    private JTextField txtPagaCon;
    private JLabel lblVuelto;
    private JComboBox<String> cbMetodoPago;
    private JComboBox<String> cbClienteVenta;
    private JTextField txtBusquedaManual;

    // Barra de estado
    private JLabel lblStatusProductos;
    private JLabel lblStatusVentas;

    private final String rolActual;
    private final String nombreUsuario;

    private List<Producto> cacheProductos = new ArrayList<>();
    private List<String> cacheTipos = new ArrayList<>();
    private List<String> cacheMarcas = new ArrayList<>();

    // Colores del sistema
    static final Color COLOR_PRIMARIO    = new Color(30, 90, 160);
    static final Color COLOR_EXITO       = new Color(39, 174, 96);
    static final Color COLOR_PELIGRO     = new Color(220, 53, 69);
    static final Color COLOR_ADVERTENCIA = new Color(243, 156, 18);
    static final Color COLOR_FONDO_BARRA = new Color(30, 40, 55);

    public VentanaProducto(String rol, String username) {
        this.rolActual = rol;
        this.nombreUsuario = username;
        iniciarVentana();
    }

    public VentanaProducto(String rol) {
        this.rolActual = rol;
        this.nombreUsuario = "Admin";
        iniciarVentana();
    }

    private void iniciarVentana() {
        recargarCaches();

        // Restaurar tema guardado (oscuro/claro)
        boolean temaOscuro = java.util.prefs.Preferences.userNodeForPackage(VentanaProducto.class)
                .getBoolean("temaOscuro", false);
        try {
            if (temaOscuro) UIManager.setLookAndFeel(new FlatDarkLaf());
            else             UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {}

        setTitle("Sistema POS — " + nombreUsuario + " [" + rolActual + "]");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setMinimumSize(new Dimension(900, 600));

        // Confirmar antes de cerrar
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) { intentarCerrar(); }
        });

        // Backup automático cada 30 minutos
        javax.swing.Timer timerBackup = new javax.swing.Timer(30 * 60 * 1000, e -> {
            String ruta = com.tienda.util.BackupDB.realizarBackup();
            if (ruta != null) System.out.println("Backup automático: " + ruta);
        });
        timerBackup.setRepeats(true);
        timerBackup.start();

        pestanas = new JTabbedPane();
        pestanas.setFont(new Font("SansSerif", Font.BOLD, 13));

        if ("ADMIN".equals(rolActual)) {
            pestanas.addTab("📊 Dashboard",            new PanelDashboard(pestanas, this));
            crearPestanaRegistro();
            crearPestanaInventario();
            crearPestanaVentas();
            pestanas.addTab("👥 Cuentas Corrientes",   new PanelClientes(nombreUsuario, cbClienteVenta));
            pestanas.addTab("📋 Historial de Ventas",  new PanelHistorial());
            pestanas.addTab("📊 Reporte Cierres",      new PanelCierresCaja());
            pestanas.addTab("🛡️ Gestión Empleados",    new PanelUsuarios(nombreUsuario));
        } else {
            crearPestanaVentas();
            pestanas.addTab("👥 Cuentas Corrientes",   new PanelClientes(nombreUsuario, cbClienteVenta));
            crearPestanaInventario();
        }

        add(crearBarraSuperior(), BorderLayout.NORTH);
        add(pestanas, BorderLayout.CENTER);
        add(crearBarraEstado(), BorderLayout.SOUTH);

        // Atajos de teclado globales
        configurarAtajosTeclado();
        SwingUtilities.updateComponentTreeUI(this);
    }

    // ── Barra superior ────────────────────────────────────────────────────────
    private JPanel crearBarraSuperior() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(COLOR_FONDO_BARRA);
        bar.setBorder(new EmptyBorder(8, 15, 8, 15));

        // Izquierda: botones
        JPanel izq = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        izq.setOpaque(false);

        JButton btnCerrar = crearBotonBarra("🚪 Cerrar Sesión", COLOR_PELIGRO);
        boolean temaOscuroActual = java.util.prefs.Preferences.userNodeForPackage(VentanaProducto.class)
                .getBoolean("temaOscuro", false);
        JToggleButton btnTema = new JToggleButton(temaOscuroActual ? "☀️ Modo Claro" : "🌙 Modo Oscuro");
        btnTema.setSelected(temaOscuroActual);
        btnTema.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnTema.setFocusPainted(false);
        btnTema.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnTema.setForeground(Color.WHITE);
        btnTema.setBackground(new Color(60, 75, 95));
        btnTema.setBorderPainted(false);

        btnCerrar.addActionListener(e -> intentarCerrar());
        btnTema.addActionListener(e -> aplicarTema(btnTema.isSelected(), btnTema));

        izq.add(btnCerrar);
        izq.add(btnTema);

        // Centro: título
        JLabel lblUser = new JLabel("Sistema POS  ·  " + nombreUsuario + " [" + rolActual + "]", SwingConstants.CENTER);
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblUser.setForeground(new Color(200, 215, 235));

        // Derecha: reloj
        JLabel lblReloj = new JLabel();
        lblReloj.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblReloj.setForeground(new Color(150, 200, 255));
        lblReloj.setHorizontalAlignment(SwingConstants.RIGHT);
        Timer timer = new Timer(1000, e -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy  HH:mm:ss");
            lblReloj.setText("📅 " + sdf.format(new java.util.Date()));
        });
        // Mostrar la hora inmediatamente sin esperar el primer tick
        lblReloj.setText("📅 " + new java.text.SimpleDateFormat("dd/MM/yyyy  HH:mm:ss").format(new java.util.Date()));
        timer.start();

        bar.add(izq, BorderLayout.WEST);
        bar.add(lblUser, BorderLayout.CENTER);
        bar.add(lblReloj, BorderLayout.EAST);
        return bar;
    }

    private JButton crearBotonBarra(String texto, Color bg) {
        JButton b = new JButton(texto);
        b.setFont(new Font("SansSerif", Font.BOLD, 12));
        b.setBackground(bg);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Barra de estado inferior ──────────────────────────────────────────────
    private JPanel crearBarraEstado() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(new Color(235, 238, 245));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 205, 215)),
                new EmptyBorder(4, 15, 4, 15)));

        lblStatusProductos = new JLabel("📦 " + cacheProductos.size() + " productos en catálogo");
        lblStatusProductos.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatusProductos.setForeground(new Color(80, 90, 110));

        lblStatusVentas = new JLabel("💳 Turno en curso  |  Atajos: F1=Ventas  F2=Inventario  F3=Registrar  ESC=Limpiar");
        lblStatusVentas.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblStatusVentas.setForeground(new Color(80, 90, 110));
        lblStatusVentas.setHorizontalAlignment(SwingConstants.RIGHT);

        bar.add(lblStatusProductos, BorderLayout.WEST);
        bar.add(lblStatusVentas, BorderLayout.EAST);
        return bar;
    }

    // ── Atajos de teclado ─────────────────────────────────────────────────────
    private void configurarAtajosTeclado() {
        JRootPane root = getRootPane();
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F1, 0), "ventas");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0), "inventario");
        root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), "registrar");

        root.getActionMap().put("ventas",     new AbstractAction() { @Override
 public void actionPerformed(ActionEvent e) { irAPestana("Caja"); } });
        root.getActionMap().put("inventario", new AbstractAction() { @Override
 public void actionPerformed(ActionEvent e) { irAPestana("Inventario"); } });
        root.getActionMap().put("registrar",  new AbstractAction() { @Override
 public void actionPerformed(ActionEvent e) { irAPestana("Registrar"); } });
    }

    private void irAPestana(String substring) {
        for (int i = 0; i < pestanas.getTabCount(); i++) {
            if (pestanas.getTitleAt(i).contains(substring)) {
                pestanas.setSelectedIndex(i);
                return;
            }
        }
    }

    private void recargarCaches() {
        cacheProductos = ProductoDAO.obtenerTodos();
        cacheTipos     = ProductoDAO.obtenerTipos();
        cacheMarcas    = ProductoDAO.obtenerMarcas();
        if (lblStatusProductos != null)
            lblStatusProductos.setText("📦 " + cacheProductos.size() + " productos en catálogo");
    }

    /** Permite al Dashboard obtener el sorter DESPUÉS de que fue inicializado */
    public TableRowSorter<DefaultTableModel> getSorterInventario() {
        return sorterInventario;
    }

    private void aplicarTema(boolean oscuro, JToggleButton btn) {
        try {
            if (oscuro) { UIManager.setLookAndFeel(new FlatDarkLaf()); btn.setText("☀️ Modo Claro"); }
            else         { UIManager.setLookAndFeel(new FlatLightLaf()); btn.setText("🌙 Modo Oscuro"); }
            // Guardar preferencia para próximo inicio
            java.util.prefs.Preferences.userNodeForPackage(VentanaProducto.class)
                    .putBoolean("temaOscuro", oscuro);
            FlatLaf.updateUI();
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) { System.err.println("Error cambiando tema: " + ex.getMessage()); }
    }

    private void intentarCerrar() {
        Map<String, Double> totales = com.tienda.db.VentaDAO.obtenerTotalesCajaAbierta();
        double pagosFiados = com.tienda.db.ClienteDAO.obtenerTotalPagosCajaAbierta();
        if (!totales.isEmpty() || pagosFiados > 0) {
            Object[] opts = {"Cerrar Caja y Salir", "Salir sin cerrar", "Cancelar"};
            int opt = JOptionPane.showOptionDialog(this,
                    "Hay movimientos en la caja actual.\n¿Deseas hacer el Cierre de Turno antes de salir?",
                    "Caja Abierta", JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.WARNING_MESSAGE, null, opts, opts[0]);
            if (opt == 0) { mostrarDialogoCierreCaja(); }
            else if (opt == 1) { dispose(); new VentanaLogin().setVisible(true); }
        } else {
            dispose(); new VentanaLogin().setVisible(true);
        }
    }

    // ── PESTAÑA REGISTRAR NUEVO ───────────────────────────────────────────────
    private void crearPestanaRegistro() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBorder(new EmptyBorder(20, 100, 20, 100));

        // Título
        JLabel lbl = new JLabel("➕ Registrar Nuevo Producto", SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", Font.BOLD, 20));
        lbl.setBorder(new EmptyBorder(0, 0, 10, 0));
        panel.add(lbl, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridLayout(7, 2, 12, 14));
        form.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(20, 25, 20, 25)));
        Font f = new Font("SansSerif", Font.PLAIN, 14);

        txtCodigoBarras = new JTextField(); txtCodigoBarras.setFont(f);
        txtNombre = new JTextField(); txtNombre.setFont(f);
        txtTipo   = new JTextField(); txtTipo.setFont(f);
        txtMarca  = new JTextField(); txtMarca.setFont(f);
        txtCosto  = new JTextField(); txtCosto.setFont(f);
        txtStock  = new JTextField(); txtStock.setFont(f);
        comboMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});
        comboMargen.setFont(f);

        JPanel panelTipo  = crearBuscadorConDesplegable(txtTipo,  () -> cacheTipos,  false);
        JPanel panelMarca = crearBuscadorConDesplegable(txtMarca, () -> cacheMarcas, false);

        Font lf = new Font("SansSerif", Font.BOLD, 13);
        form.add(label("Código de Barras:", lf)); form.add(txtCodigoBarras);
        form.add(label("Nombre del Producto:", lf)); form.add(txtNombre);
        form.add(label("Tipo:", lf)); form.add(panelTipo);
        form.add(label("Marca:", lf)); form.add(panelMarca);
        form.add(label("Precio de Costo ($):", lf)); form.add(txtCosto);
        form.add(label("Stock Inicial:", lf)); form.add(txtStock);
        form.add(label("Margen de Ganancia (%):", lf)); form.add(comboMargen);

        // Botones
        JButton btnGuardar = new JButton("💾 GUARDAR PRODUCTO");
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnGuardar.setBackground(COLOR_EXITO);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setPreferredSize(new Dimension(0, 46));
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> ejecutarGuardado());

        JButton btnLimpiar = new JButton("🗑️ Limpiar Campos");
        btnLimpiar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnLimpiar.setFocusPainted(false);
        btnLimpiar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiar.addActionListener(e -> limpiarFormularioRegistro());

        JButton btnImportar = new JButton("📂 Importar Catálogo SEPA");
        btnImportar.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnImportar.setFocusPainted(false);
        btnImportar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnImportar.addActionListener(e -> importarSEPA());

        JPanel panelBotones = new JPanel(new GridLayout(1, 3, 10, 0));
        panelBotones.setBorder(new EmptyBorder(12, 0, 0, 0));
        panelBotones.add(btnImportar);
        panelBotones.add(btnLimpiar);
        panelBotones.add(btnGuardar);

        // Flujo de Enter entre campos
        txtCodigoBarras.addActionListener(e -> {
            String codigo = txtCodigoBarras.getText().trim();
            if (codigo.isEmpty()) return;
            if (ProductoDAO.buscarPorCodigo(codigo) != null) {
                JOptionPane.showMessageDialog(this, "⚠️ Este producto ya existe en el sistema.\nPodés editarlo desde el inventario.",
                        "Código Duplicado", JOptionPane.WARNING_MESSAGE);
                txtCodigoBarras.selectAll();
                return;
            }
            // 1° buscar en catálogo SEPA local (rápido, sin internet)
            String[] datosCatalogo = com.tienda.db.CatalogoDAO.buscarEnCatalogo(codigo);
            if (datosCatalogo != null) {
                txtNombre.setText(datosCatalogo[0]);
                txtMarca.setText(datosCatalogo[1]);
                txtTipo.setText(datosCatalogo[2]);
                JOptionPane.showMessageDialog(this, "✅ Datos completados desde catálogo SEPA.\nVerificá precio y stock.",
                        "Autocompletado", JOptionPane.INFORMATION_MESSAGE);
                txtCosto.requestFocus();
                return;
            }
            // 2° fallback: buscar en internet
            txtNombre.setText("Buscando...");
            new Thread(() -> {
                var datos = com.tienda.util.BuscadorProducto.buscarPorCodigo(codigo);
                SwingUtilities.invokeLater(() -> {
                    if (datos.encontrado()) {
                        txtNombre.setText(datos.nombre != null ? datos.nombre : "");
                        txtMarca.setText(datos.marca != null ? datos.marca : "");
                        txtTipo.setText(datos.tipo != null ? datos.tipo : "");
                        JOptionPane.showMessageDialog(this, "✅ Datos autocompletados desde internet.\nVerificá y completá precio y stock.",
                                "Autocompletado", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        txtNombre.setText("");
                        JOptionPane.showMessageDialog(this, "Producto no encontrado.\nCompletá los datos manualmente.",
                                "No encontrado", JOptionPane.INFORMATION_MESSAGE);
                    }
                    txtNombre.requestFocus();
                });
            }).start();
        });
        txtNombre.addActionListener(e -> txtTipo.requestFocus());
        txtTipo.addActionListener(e -> txtMarca.requestFocus());
        txtMarca.addActionListener(e -> txtCosto.requestFocus());
        txtCosto.addActionListener(e -> txtStock.requestFocus());
        txtStock.addActionListener(e -> comboMargen.requestFocus());
        comboMargen.addKeyListener(new KeyAdapter() {
            @Override

            public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) btnGuardar.doClick(); }
        });

        panel.add(form, BorderLayout.CENTER);
        panel.add(panelBotones, BorderLayout.SOUTH);
        pestanas.addTab("➕ Registrar Nuevo", panel);
    }

    private JLabel label(String texto, Font f) {
        JLabel l = new JLabel(texto);
        l.setFont(f);
        return l;
    }

    private void limpiarFormularioRegistro() {
        txtCodigoBarras.setText(""); txtNombre.setText(""); txtCosto.setText("");
        txtStock.setText(""); txtTipo.setText(""); txtMarca.setText("");
        comboMargen.setSelectedIndex(1);
        txtCodigoBarras.requestFocus();
    }

    private void importarSEPA() {
        // Primero preguntar destino
        int opcion = JOptionPane.showOptionDialog(this,
                "Como queres importar los productos SEPA?\n\n" +
                "1) Catalogo oculto: solo para autocompletar al registrar productos\n" +
                "   (recomendado - el inventario no se llena de productos no usados)\n\n" +
                "2) Inventario activo: agrega todos al stock con cantidad 0",
                "Destino de importacion",
                JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"1) Catalogo oculto", "2) Inventario activo", "Cancelar"}, "1) Catalogo oculto");
        if (opcion == 2 || opcion < 0) return;

        // Despues seleccionar archivo
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccioná el archivo SEPA (.csv)");
        fc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("Archivos CSV", "csv"));
        if (fc.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) return;
        String ruta = fc.getSelectedFile().getAbsolutePath();

        String margenStr = JOptionPane.showInputDialog(this,
                "Margen de ganancia a aplicar (%)", "30");
        if (margenStr == null) return;
        double margen;
        try { margen = Double.parseDouble(margenStr.replace(",", ".")); }
        catch (Exception ex) { JOptionPane.showMessageDialog(this, "Margen invalido."); return; }

        JDialog dlg = new JDialog(this, "Importando...", false);
        JLabel lbl = new JLabel("  Procesando archivo, espera un momento...  ");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lbl.setBorder(new EmptyBorder(20, 30, 20, 30));
        dlg.add(lbl); dlg.pack(); dlg.setLocationRelativeTo(this); dlg.setVisible(true);

        final int destino = opcion;
        double margenFinal = margen;
        new Thread(() -> {
            var resultado = destino == 0
                    ? com.tienda.util.ImportadorSEPA.importar(ruta, margenFinal)
                    : com.tienda.util.ImportadorSEPA.importarAlInventario(ruta, margenFinal);
            SwingUtilities.invokeLater(() -> {
                dlg.dispose();
                recargarCaches(); actualizarTabla();
                String destLabel = destino == 0 ? "catalogo oculto" : "inventario activo";
                JOptionPane.showMessageDialog(this,
                        "Importacion completada al " + destLabel + "!\n\n" +
                        "Importados: " + resultado.importados + "\n" +
                        "Duplicados saltados: " + resultado.duplicados + "\n" +
                        "Errores: " + resultado.errores,
                        "Resultado", JOptionPane.INFORMATION_MESSAGE);
            });
        }).start();
    }

    // ── PESTAÑA INVENTARIO ────────────────────────────────────────────────────
    private void crearPestanaInventario() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Barra de búsqueda y filtros
        JPanel topBar = new JPanel(new BorderLayout(8, 0));

        JPanel busqPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel lblBuscar = new JLabel("🔍");
        lblBuscar.setFont(new Font("SansSerif", Font.PLAIN, 16));
        JTextField txtBuscador = new JTextField(30);
        txtBuscador.putClientProperty("JTextField.placeholderText", "Buscar por nombre, código, marca...");
        txtBuscador.setFont(new Font("SansSerif", Font.PLAIN, 14));
        busqPanel.add(lblBuscar); busqPanel.add(txtBuscador);

        JPanel botonesPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnFiltroStockBajo = new JButton("⚠️ Solo Stock Bajo");
        btnFiltroStockBajo.setFocusPainted(false);
        btnFiltroStockBajo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton btnTodos = new JButton("📋 Todos");
        btnTodos.setFocusPainted(false);
        btnTodos.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JButton btnActualizar = new JButton("↻ Actualizar");
        btnActualizar.setFocusPainted(false);
        btnActualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        botonesPanel.add(btnFiltroStockBajo); botonesPanel.add(btnTodos); botonesPanel.add(btnActualizar);
        topBar.add(busqPanel, BorderLayout.WEST);
        topBar.add(botonesPanel, BorderLayout.EAST);
        topBar.setBorder(new EmptyBorder(0, 0, 6, 0));
        panel.add(topBar, BorderLayout.NORTH);

        // Tabla
        String[] columnas = {"CÓDIGO", "NOMBRE", "TIPO", "MARCA", "COSTO", "VENTA", "STOCK"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override

            public boolean isCellEditable(int r, int c) { return false; }
            @Override

            public Class<?> getColumnClass(int c) { return String.class; }
        };
        tabla = new JTable(modeloTabla);
        tabla.setRowHeight(32);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(new Color(45, 55, 72));
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.setSelectionBackground(new Color(66, 153, 225));
        tabla.setGridColor(new Color(220, 225, 235));
        tabla.setShowGrid(true);
        tabla.setIntercellSpacing(new Dimension(1, 1));

        // Renderer: stock bajo en rojo, filas alternas
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override

            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                if (!sel) {
                    try {
                        int stock = Integer.parseInt(t.getValueAt(row, 6).toString());
                        if (stock <= 5) {
                            c.setBackground(new Color(255, 235, 238));
                            c.setForeground(new Color(180, 30, 40));
                        } else {
                            c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                            c.setForeground(t.getForeground());
                        }
                    } catch (Exception ex) {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                        c.setForeground(t.getForeground());
                    }
                }
                return c;
            }
        });

        // Ajustar anchos de columnas
        tabla.getColumnModel().getColumn(0).setPreferredWidth(130);
        tabla.getColumnModel().getColumn(1).setPreferredWidth(250);
        tabla.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabla.getColumnModel().getColumn(4).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(5).setPreferredWidth(90);
        tabla.getColumnModel().getColumn(6).setPreferredWidth(75);

        sorterInventario = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorterInventario);
        actualizarTabla();

        // Búsqueda en tiempo real
        txtBuscador.addKeyListener(new KeyAdapter() {
            @Override

            public void keyReleased(KeyEvent e) {
                String text = txtBuscador.getText().trim();
                if (text.isEmpty()) sorterInventario.setRowFilter(null);
                else sorterInventario.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        // Filtros
        btnFiltroStockBajo.addActionListener(e -> sorterInventario.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override

            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                try { return Integer.parseInt(entry.getModel().getValueAt(entry.getIdentifier(), 6).toString()) <= 5; }
                catch (Exception ex) { return false; }
            }
        }));
        btnTodos.addActionListener(e -> { txtBuscador.setText(""); sorterInventario.setRowFilter(null); });
        btnActualizar.addActionListener(e -> { txtBuscador.setText(""); sorterInventario.setRowFilter(null); recargarCaches(); actualizarTabla(); });

        // Menú contextual (solo ADMIN)
        if ("ADMIN".equals(rolActual)) {
            JPopupMenu popup = new JPopupMenu();
            JMenuItem itemEditar   = new JMenuItem("✏️ Editar Producto");
            JMenuItem itemEliminar = new JMenuItem("🗑️ Eliminar Producto(s)");
            popup.add(itemEditar); popup.addSeparator(); popup.add(itemEliminar);
            tabla.setComponentPopupMenu(popup);

            itemEditar.addActionListener(e -> abrirEdicionDesdeTabla());
            itemEliminar.addActionListener(e -> {
                int[] filas = tabla.getSelectedRows();
                if (filas.length == 0) return;
                if (JOptionPane.showConfirmDialog(this,
                        "¿Eliminar " + filas.length + " producto(s) seleccionado(s)?",
                        "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    for (int i : filas)
                        ProductoDAO.eliminarProducto(modeloTabla.getValueAt(tabla.convertRowIndexToModel(i), 0).toString());
                    recargarCaches(); actualizarTabla();
                    JOptionPane.showMessageDialog(this, "✅ Productos eliminados.");
                }
            });
            tabla.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override

                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) abrirEdicionDesdeTabla();
                }
            });
        }

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225)));
        panel.add(scroll, BorderLayout.CENTER);

        // Etiqueta con conteo
        JLabel lblConteo = new JLabel();
        lblConteo.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblConteo.setForeground(new Color(100, 110, 130));
        lblConteo.setBorder(new EmptyBorder(4, 0, 0, 0));
        panel.add(lblConteo, BorderLayout.SOUTH);

        // Actualizar conteo al filtrar
        sorterInventario.addRowSorterListener(e -> lblConteo.setText(
                "Mostrando " + tabla.getRowCount() + " de " + modeloTabla.getRowCount() + " productos"));
        lblConteo.setText("Mostrando " + tabla.getRowCount() + " de " + modeloTabla.getRowCount() + " productos");

        pestanas.addTab("📦 Ver Inventario", panel);
    }

    // ── PESTAÑA VENTAS ────────────────────────────────────────────────────────
    private void crearPestanaVentas() {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBorder(new EmptyBorder(10, 12, 10, 12));

        // Barra de escaneo
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 5));
        topBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        topBar.setBackground(new Color(248, 250, 253));

        JTextField txtEscaneo = new JTextField(18);
        txtEscaneo.setFont(new Font("SansSerif", Font.BOLD, 15));
        txtEscaneo.putClientProperty("JTextField.placeholderText", "Escanear código...");
        txtEscaneo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_PRIMARIO, 2, true),
                new EmptyBorder(6, 8, 6, 8)));
        txtEscaneo.addFocusListener(new FocusAdapter() {
            @Override

            public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtEscaneo.selectAll()); }
        });

        txtBusquedaManual = new JTextField(28);
        txtBusquedaManual.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtBusquedaManual.putClientProperty("JTextField.placeholderText", "Buscar producto manualmente...");
        JPanel panelBusq = crearBuscadorConDesplegable(txtBusquedaManual, () -> obtenerListaProductosFormateada(), true);

        JLabel lbl1 = new JLabel("📷 Código:");
        lbl1.setFont(new Font("SansSerif", Font.BOLD, 13));
        JLabel lbl2 = new JLabel("🔍 Buscar:");
        lbl2.setFont(new Font("SansSerif", Font.BOLD, 13));

        // Botón limpiar carrito
        JButton btnLimpiarCarrito = new JButton("🗑️ Limpiar Carrito");
        btnLimpiarCarrito.setFont(new Font("SansSerif", Font.PLAIN, 12));
        btnLimpiarCarrito.setForeground(COLOR_PELIGRO);
        btnLimpiarCarrito.setFocusPainted(false);
        btnLimpiarCarrito.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnLimpiarCarrito.addActionListener(e -> {
            if (modeloVenta.getRowCount() == 0) return;
            if (JOptionPane.showConfirmDialog(this, "¿Limpiar todo el carrito?",
                    "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                modeloVenta.setRowCount(0);
                totalVenta = 0;
                lblTotal.setText("TOTAL: $0.00");
                txtPagaCon.setText("");
                lblVuelto.setText("Vuelto: $0.00");
            }
        });

        topBar.add(lbl1); topBar.add(txtEscaneo);
        topBar.add(Box.createHorizontalStrut(10));
        topBar.add(lbl2); topBar.add(panelBusq);
        topBar.add(Box.createHorizontalStrut(20));
        topBar.add(btnLimpiarCarrito);
        panel.add(topBar, BorderLayout.NORTH);

        // Tabla carrito
        modeloVenta = new DefaultTableModel(new String[]{"CÓDIGO", "PRODUCTO", "PRECIO UNIT.", "CANT.", "SUBTOTAL"}, 0) {
            @Override

            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaVenta = new JTable(modeloVenta);
        tablaVenta.setRowHeight(34);
        tablaVenta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        tablaVenta.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tablaVenta.getTableHeader().setBackground(new Color(45, 55, 72));
        tablaVenta.getTableHeader().setForeground(Color.WHITE);
        tablaVenta.setSelectionBackground(new Color(66, 153, 225));

        tablaVenta.getColumnModel().getColumn(0).setPreferredWidth(120);
        tablaVenta.getColumnModel().getColumn(1).setPreferredWidth(280);
        tablaVenta.getColumnModel().getColumn(2).setPreferredWidth(110);
        tablaVenta.getColumnModel().getColumn(3).setPreferredWidth(70);
        tablaVenta.getColumnModel().getColumn(4).setPreferredWidth(110);

        // Controles cantidad
        JPanel panelControles = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        panelControles.setBackground(new Color(248, 250, 253));
        panelControles.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(210, 215, 225)));
        JButton btnSumar  = new JButton("＋");
        JButton btnRestar = new JButton("－");
        JButton btnQuitar = new JButton("✕ Quitar");
        for (JButton b : new JButton[]{btnSumar, btnRestar, btnQuitar}) {
            b.setFont(new Font("SansSerif", Font.BOLD, 13));
            b.setFocusPainted(false);
            b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        btnSumar.setBackground(COLOR_EXITO); btnSumar.setForeground(Color.WHITE); btnSumar.setBorderPainted(false);
        btnRestar.setBackground(COLOR_ADVERTENCIA); btnRestar.setForeground(Color.WHITE); btnRestar.setBorderPainted(false);
        btnQuitar.setBackground(COLOR_PELIGRO); btnQuitar.setForeground(Color.WHITE); btnQuitar.setBorderPainted(false);

        btnSumar.addActionListener(e -> ajustarCantidadCarrito(1));
        btnRestar.addActionListener(e -> ajustarCantidadCarrito(-1));
        btnQuitar.addActionListener(e -> ajustarCantidadCarrito(0));

        JLabel lblInstruccion = new JLabel("  Seleccioná fila y ajustá cantidad:");
        lblInstruccion.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblInstruccion.setForeground(new Color(100, 110, 130));
        panelControles.add(lblInstruccion);
        panelControles.add(btnSumar); panelControles.add(btnRestar); panelControles.add(btnQuitar);

        JPanel centroCentro = new JPanel(new BorderLayout());
        JScrollPane scrollCarrito = new JScrollPane(tablaVenta);
        scrollCarrito.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225)));
        centroCentro.add(scrollCarrito, BorderLayout.CENTER);
        centroCentro.add(panelControles, BorderLayout.SOUTH);
        panel.add(centroCentro, BorderLayout.CENTER);

        // Panel pago
        cbClienteVenta = new JComboBox<>();
        cbMetodoPago   = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cuenta Corriente (Fiado)"});
        cbMetodoPago.setFont(new Font("SansSerif", Font.PLAIN, 14));
        cbClienteVenta.setFont(new Font("SansSerif", Font.PLAIN, 14));
        txtPagaCon = new JTextField();
        txtPagaCon.setFont(new Font("SansSerif", Font.PLAIN, 15));
        txtPagaCon.putClientProperty("JTextField.placeholderText", "0.00");
        txtPagaCon.addFocusListener(new FocusAdapter() {
            @Override

            public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtPagaCon.selectAll()); }
        });
        lblVuelto = new JLabel("Vuelto: $0.00");
        lblVuelto.setFont(new Font("SansSerif", Font.BOLD, 20));
        lblVuelto.setForeground(COLOR_EXITO);

        JPanel panelPago = new JPanel(new GridLayout(2, 4, 12, 8));
        panelPago.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 210, 225), 1, true),
                new EmptyBorder(10, 15, 10, 15)));
        panelPago.setBackground(new Color(248, 250, 253));
        Font pf = new Font("SansSerif", Font.BOLD, 12);
        panelPago.add(label("Cliente:", pf)); panelPago.add(label("Método de Pago:", pf));
        panelPago.add(label("El cliente abona ($):", pf)); panelPago.add(label("", pf));
        panelPago.add(cbClienteVenta); panelPago.add(cbMetodoPago);
        panelPago.add(txtPagaCon); panelPago.add(lblVuelto);

        // Total y finalizar
        lblTotal = new JLabel("TOTAL: $0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblTotal.setForeground(COLOR_PRIMARIO);

        JButton btnFinalizar = new JButton("✅ CONFIRMAR VENTA");
        btnFinalizar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnFinalizar.setBackground(COLOR_EXITO);
        btnFinalizar.setForeground(Color.WHITE);
        btnFinalizar.setFocusPainted(false);
        btnFinalizar.setBorderPainted(false);
        btnFinalizar.setPreferredSize(new Dimension(220, 50));
        btnFinalizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnFinalizar.addActionListener(e -> { if (totalVenta > 0) finalizarVenta(); });

        JButton btnCierreCaja = new JButton("🔒 Cierre de Caja");
        btnCierreCaja.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnCierreCaja.setBackground(COLOR_ADVERTENCIA);
        btnCierreCaja.setForeground(Color.WHITE);
        btnCierreCaja.setFocusPainted(false);
        btnCierreCaja.setBorderPainted(false);
        btnCierreCaja.setPreferredSize(new Dimension(180, 50));
        btnCierreCaja.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnCierreCaja.addActionListener(e -> mostrarDialogoCierreCaja());

        JPanel panelAcciones = new JPanel(new BorderLayout(15, 0));
        panelAcciones.setOpaque(false);
        panelAcciones.add(btnCierreCaja, BorderLayout.WEST);
        JPanel panelTotalFin = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 0));
        panelTotalFin.setOpaque(false);
        panelTotalFin.add(lblTotal); panelTotalFin.add(btnFinalizar);
        panelAcciones.add(panelTotalFin, BorderLayout.EAST);

        JPanel sur = new JPanel(new BorderLayout(0, 10));
        sur.add(panelPago, BorderLayout.CENTER);
        sur.add(panelAcciones, BorderLayout.SOUTH);
        panel.add(sur, BorderLayout.SOUTH);

        // Listeners
        txtEscaneo.addActionListener(e -> {
            String cod = txtEscaneo.getText().trim();
            if (cod.isEmpty()) return;
            Producto p = ProductoDAO.buscarPorCodigo(cod);
            if (p != null && p.getStock() > 0) { agregarAlCarrito(p); txtEscaneo.setText(""); }
            else JOptionPane.showMessageDialog(this, "❌ Producto sin stock o no registrado.");
            txtEscaneo.requestFocus();
        });
        txtPagaCon.addKeyListener(new KeyAdapter() {
            @Override

            public void keyReleased(KeyEvent e) { calcularVuelto(); }
        });

        pestanas.addTab("🛒 Caja y Ventas", panel);
    }

    // ── Helpers carrito ───────────────────────────────────────────────────────
    private List<String> obtenerListaProductosFormateada() {
        List<String> lista = new ArrayList<>();
        for (Producto p : cacheProductos)
            lista.add(p.getCodigoBarras() + " - " + p.getTipo() + " " + p.getMarca() + " " + p.getNombre());
        return lista;
    }

    private JPanel crearBuscadorConDesplegable(JTextField tf, java.util.function.Supplier<List<String>> proveedor, boolean isVentas) {
        JPanel p = new JPanel(new BorderLayout());
        p.add(tf, BorderLayout.CENTER);
        JButton btn = new JButton("▾");
        btn.setPreferredSize(new Dimension(30, 0));
        btn.setFocusable(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        p.add(btn, BorderLayout.EAST);

        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);

        java.util.function.Consumer<String> mostrar = filtro -> {
            popup.removeAll();
            String f = filtro.toLowerCase().trim();
            int n = 0;
            for (String op : proveedor.get()) {
                if (f.isEmpty() || op.toLowerCase().contains(f)) {
                    JMenuItem item = new JMenuItem(op);
                    item.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                    item.addMouseListener(new java.awt.event.MouseAdapter() {
                        @Override
                        public void mousePressed(java.awt.event.MouseEvent e) {
                            if (isVentas) { tf.setText(""); popup.setVisible(false); procesarSeleccionVenta(op); }
                            else { tf.setText(op); popup.setVisible(false); }
                            tf.requestFocus();
                        }
                    });
                    popup.add(item);
                    if (++n >= 15) break;
                }
            }
            if (n > 0) { popup.show(tf, 0, tf.getHeight()); tf.requestFocus(); }
            else popup.setVisible(false);
        };

        tf.addKeyListener(new KeyAdapter() {
            @Override

            public void keyReleased(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_ESCAPE) { popup.setVisible(false); return; }
                if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_UP || c == KeyEvent.VK_DOWN) return;
                mostrar.accept(tf.getText());
            }
        });
        btn.addActionListener(e -> { tf.requestFocus(); mostrar.accept(""); });
        tf.addFocusListener(new FocusAdapter() {
            @Override

            public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> tf.selectAll()); }
        });
        if (isVentas) {
            tf.addActionListener(e -> {
                String t = tf.getText().trim().toLowerCase();
                if (t.isEmpty()) return;
                for (String op : proveedor.get()) {
                    if (op.toLowerCase().contains(t)) { procesarSeleccionVenta(op); tf.setText(""); popup.setVisible(false); return; }
                }
            });
        }
        return p;
    }

    private void procesarSeleccionVenta(String sel) {
        if (!sel.contains(" - ")) return;
        String cod = sel.split(" - ")[0];
        Producto p = ProductoDAO.buscarPorCodigo(cod);
        if (p != null && p.getStock() > 0) agregarAlCarrito(p);
        else JOptionPane.showMessageDialog(this, "❌ Sin stock disponible.");
    }

    private void agregarAlCarrito(Producto p) {
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            if (modeloVenta.getValueAt(i, 0).equals(p.getCodigoBarras())) {
                int cant = (int) modeloVenta.getValueAt(i, 3);
                if (cant >= p.getStock()) { JOptionPane.showMessageDialog(this, "⚠️ No hay más stock disponible."); return; }
                modeloVenta.setValueAt(cant + 1, i, 3);
                modeloVenta.setValueAt((cant + 1) * p.getPrecioVenta(), i, 4);
                totalVenta += p.getPrecioVenta();
                lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta));
                calcularVuelto(); return;
            }
        }
        modeloVenta.addRow(new Object[]{p.getCodigoBarras(), p.getNombre(), p.getPrecioVenta(), 1, p.getPrecioVenta()});
        totalVenta += p.getPrecioVenta();
        lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta));
        calcularVuelto();
    }

    private void ajustarCantidadCarrito(int cambio) {
        int[] filas = tablaVenta.getSelectedRows();
        if (filas.length == 0) { JOptionPane.showMessageDialog(this, "Seleccioná un producto en el carrito."); return; }
        for (int i = filas.length - 1; i >= 0; i--) {
            int fila = filas[i];
            int cant = (int) modeloVenta.getValueAt(fila, 3);
            double precio = (double) modeloVenta.getValueAt(fila, 2);
            if (cambio == 0 || cant + cambio <= 0) {
                totalVenta -= cant * precio; modeloVenta.removeRow(fila);
            } else {
                if (cambio > 0) {
                    Producto p = ProductoDAO.buscarPorCodigo(modeloVenta.getValueAt(fila, 0).toString());
                    if (p != null && cant + cambio > p.getStock()) continue;
                }
                modeloVenta.setValueAt(cant + cambio, fila, 3);
                modeloVenta.setValueAt((cant + cambio) * precio, fila, 4);
                totalVenta += cambio * precio;
            }
        }
        lblTotal.setText("TOTAL: $" + String.format("%.2f", Math.max(0, totalVenta)));
        if (totalVenta < 0) totalVenta = 0;
        calcularVuelto();
    }

    private void calcularVuelto() {
        try {
            String txt = txtPagaCon.getText().trim();
            if (txt.isEmpty()) { lblVuelto.setText("Vuelto: $0.00"); lblVuelto.setForeground(COLOR_EXITO); return; }
            double abona  = Double.parseDouble(txt);
            double vuelto = abona - totalVenta;
            if (vuelto < 0) {
                lblVuelto.setText("FALTAN: $" + String.format("%.2f", Math.abs(vuelto)));
                lblVuelto.setForeground(COLOR_PELIGRO);
            } else {
                lblVuelto.setText("Vuelto: $" + String.format("%.2f", vuelto));
                lblVuelto.setForeground(COLOR_EXITO);
            }
        } catch (Exception ex) {
            lblVuelto.setText("Vuelto: $0.00");
            lblVuelto.setForeground(COLOR_EXITO);
        }
    }

    private void finalizarVenta() {
        if (totalVenta <= 0) { JOptionPane.showMessageDialog(this, "El carrito está vacío."); return; }

        String metodo         = cbMetodoPago.getSelectedItem().toString();
        String clienteCompleto = cbClienteVenta.getSelectedItem() != null ? cbClienteVenta.getSelectedItem().toString() : "0 - Consumidor Final";
        String nombreCliente   = clienteCompleto.contains(" - ") ? clienteCompleto.split(" - ", 2)[1] : clienteCompleto;

        // Validar que si es fiado haya cliente seleccionado
        if (metodo.equals("Cuenta Corriente (Fiado)")) {
            if (clienteCompleto.startsWith("0 -")) {
                JOptionPane.showMessageDialog(this, "⚠️ Seleccioná un cliente válido para registrar una venta fiada.", "Cliente Requerido", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        List<String[]> productosTicket = new ArrayList<>();
        StringBuilder detalle = new StringBuilder();
        double costoTotal = 0;

        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            String cod  = modeloVenta.getValueAt(i, 0).toString();
            String nom  = modeloVenta.getValueAt(i, 1).toString();
            String prec = String.format("%.2f", (double) modeloVenta.getValueAt(i, 2));
            String cant = modeloVenta.getValueAt(i, 3).toString();
            String sub  = String.format("%.2f", (double) modeloVenta.getValueAt(i, 4));
            int cantInt = Integer.parseInt(cant);
            // Calcular costo para este ítem
            com.tienda.Modelo.Producto prod = ProductoDAO.buscarPorCodigo(cod);
            if (prod != null) costoTotal += prod.getPrecioCosto() * cantInt;
            // precio unitario de venta para el ticket
            double precioUnit = (double) modeloVenta.getValueAt(i, 2);
            productosTicket.add(new String[]{cod, nom, prec, cant, sub, String.format("%.2f", precioUnit)});
            detalle.append(cant).append("x ").append(nom).append(" | ");
            ProductoDAO.reducirStock(cod, cantInt);
        }

        if (metodo.equals("Cuenta Corriente (Fiado)")) {
            int idCliente = Integer.parseInt(clienteCompleto.split(" - ")[0]);
            com.tienda.db.ClienteDAO.registrarMovimiento(idCliente, "FIADO", totalVenta, detalle.toString());
        }

        com.tienda.db.VentaDAO.registrarVentaConCosto(detalle.toString(), totalVenta, costoTotal, metodo, nombreUsuario, nombreCliente);

        String nroTicket = String.valueOf(System.currentTimeMillis()).substring(7);
        String fecha = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date());
        double vuelto = 0;
        try {
            String pagaStr = txtPagaCon.getText().trim();
            if (!pagaStr.isEmpty()) vuelto = Double.parseDouble(pagaStr) - totalVenta;
        } catch (Exception ignored) {}
        boolean esFiado = metodo.equals("Cuenta Corriente (Fiado)");
        com.tienda.util.TicketPDF.generarTicket(nroTicket, fecha, nombreCliente, nombreUsuario,
                productosTicket, totalVenta, metodo, Math.max(0, vuelto), esFiado);

        JOptionPane.showMessageDialog(this, "✅ Venta registrada exitosamente. Generando comprobante...");
        modeloVenta.setRowCount(0);
        totalVenta = 0;
        lblTotal.setText("TOTAL: $0.00");
        txtPagaCon.setText("");
        lblVuelto.setText("Vuelto: $0.00");
        lblVuelto.setForeground(COLOR_EXITO);
        recargarCaches();
    }

    // ── Guardado de producto ──────────────────────────────────────────────────
    private void ejecutarGuardado() {
        String cod = txtCodigoBarras.getText().trim();
        String nom = txtNombre.getText().trim();

        if (cod.isEmpty() || nom.isEmpty()) {
            JOptionPane.showMessageDialog(this, "⚠️ Código y nombre son obligatorios.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (ProductoDAO.buscarPorCodigo(cod) != null) {
            JOptionPane.showMessageDialog(this, "❌ Ya existe un producto con ese código.", "Código Duplicado", JOptionPane.ERROR_MESSAGE);
            return;
        }
        try {
            double costo = Double.parseDouble(txtCosto.getText().trim().replace(",", "."));
            int    stock = Integer.parseInt(txtStock.getText().trim());
            double margen = Double.parseDouble(comboMargen.getSelectedItem().toString());
            String tipo  = txtTipo.getText().trim().isEmpty()  ? "-" : txtTipo.getText().trim();
            String marca = txtMarca.getText().trim().isEmpty() ? "-" : txtMarca.getText().trim();

            if (costo <= 0) { JOptionPane.showMessageDialog(this, "⚠️ El costo debe ser mayor a 0.", "Valor Inválido", JOptionPane.WARNING_MESSAGE); return; }
            if (stock < 0)  { JOptionPane.showMessageDialog(this, "⚠️ El stock no puede ser negativo.", "Valor Inválido", JOptionPane.WARNING_MESSAGE); return; }

            ProductoDAO.registrarProducto(new Producto(cod, nom, tipo, marca, costo, stock, margen));
            JOptionPane.showMessageDialog(this, "✅ Producto guardado correctamente.");
            limpiarFormularioRegistro();
            recargarCaches(); actualizarTabla();
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "⚠️ Costo y Stock deben ser números válidos.", "Datos Inválidos", JOptionPane.WARNING_MESSAGE);
        }
    }

    public void actualizarTabla() {
        modeloTabla.setRowCount(0);
        for (Producto p : cacheProductos)
            modeloTabla.addRow(new Object[]{
                    p.getCodigoBarras(), p.getNombre(), p.getTipo(), p.getMarca(),
                    String.format("%.2f", p.getPrecioCosto()),
                    String.format("%.2f", p.getPrecioVenta()),
                    String.valueOf(p.getStock())
            });
    }

    private void abrirEdicionDesdeTabla() {
        int fila = tabla.getSelectedRow();
        if (fila != -1) abrirDialogoEdicion(tabla.convertRowIndexToModel(fila));
    }

    private void abrirDialogoEdicion(int fila) {
        String codigo = modeloTabla.getValueAt(fila, 0).toString();
        JDialog dlg = new JDialog(this, "✏️ Editar Producto — " + codigo, true);
        dlg.setLayout(new BorderLayout(10, 10));
        dlg.setSize(430, 400);
        dlg.setLocationRelativeTo(this);
        dlg.getRootPane().setBorder(new EmptyBorder(15, 20, 15, 20));

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 12));
        Font f = new Font("SansSerif", Font.PLAIN, 14);

        JTextField tNom = new JTextField(modeloTabla.getValueAt(fila, 1).toString()); tNom.setFont(f);
        JTextField tTip = new JTextField(modeloTabla.getValueAt(fila, 2).toString().equals("-") ? "" : modeloTabla.getValueAt(fila, 2).toString()); tTip.setFont(f);
        JTextField tMar = new JTextField(modeloTabla.getValueAt(fila, 3).toString().equals("-") ? "" : modeloTabla.getValueAt(fila, 3).toString()); tMar.setFont(f);
        JTextField tCos = new JTextField(modeloTabla.getValueAt(fila, 4).toString()); tCos.setFont(f);
        JTextField tStk = new JTextField(modeloTabla.getValueAt(fila, 6).toString()); tStk.setFont(f);
        JComboBox<String> cbMrg = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"}); cbMrg.setFont(f);

        JPanel pTip = crearBuscadorConDesplegable(tTip, () -> cacheTipos,  false);
        JPanel pMar = crearBuscadorConDesplegable(tMar, () -> cacheMarcas, false);

        Font lf = new Font("SansSerif", Font.BOLD, 13);
        form.add(label("Nombre:", lf));       form.add(tNom);
        form.add(label("Tipo:", lf));         form.add(pTip);
        form.add(label("Marca:", lf));        form.add(pMar);
        form.add(label("Costo ($):", lf));    form.add(tCos);
        form.add(label("Stock:", lf));        form.add(tStk);
        form.add(label("Margen (%):", lf));   form.add(cbMrg);

        JButton btnGuardar = new JButton("💾 Guardar Cambios");
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnGuardar.setBackground(COLOR_EXITO);
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false); btnGuardar.setBorderPainted(false);
        btnGuardar.addActionListener(e -> {
            try {
                double costo = Double.parseDouble(tCos.getText().trim().replace(",", "."));
                int    stock = Integer.parseInt(tStk.getText().trim());
                if (costo <= 0 || stock < 0) throw new NumberFormatException();
                Producto ed = new Producto(codigo, tNom.getText().trim(),
                        tTip.getText().trim().isEmpty() ? "-" : tTip.getText().trim(),
                        tMar.getText().trim().isEmpty() ? "-" : tMar.getText().trim(),
                        costo, stock,
                        Double.parseDouble(cbMrg.getSelectedItem().toString()));
                ProductoDAO.actualizarProducto(ed);
                recargarCaches(); actualizarTabla();
                dlg.dispose();
                JOptionPane.showMessageDialog(this, "✅ Producto actualizado.");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "⚠️ Revisá los datos ingresados.", "Datos Inválidos", JOptionPane.WARNING_MESSAGE);
            }
        });

        dlg.add(form, BorderLayout.CENTER);
        dlg.add(btnGuardar, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    // ── Cierre de caja ────────────────────────────────────────────────────────
    private void mostrarDialogoCierreCaja() {
        Map<String, Double> totales = com.tienda.db.VentaDAO.obtenerTotalesCajaAbierta();
        double pagosFiados = com.tienda.db.ClienteDAO.obtenerTotalPagosCajaAbierta();
        if (totales.isEmpty() && pagosFiados == 0) {
            JOptionPane.showMessageDialog(this, "La caja está vacía. No hay movimientos que cerrar.", "Aviso", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        String inputFondo = JOptionPane.showInputDialog(this, "¿Con cuánto fondo inicial abriste el turno? ($)", "0.00");
        if (inputFondo == null) return;
        double fondo;
        try { fondo = Double.parseDouble(inputFondo.replace(",", ".")); }
        catch (Exception e) { JOptionPane.showMessageDialog(this, "Monto inválido.", "Error", JOptionPane.ERROR_MESSAGE); return; }

        double efectivo = totales.getOrDefault("Efectivo", 0.0);
        double fiadas   = totales.getOrDefault("Cuenta Corriente (Fiado)", 0.0);
        double digital  = 0;
        for (Map.Entry<String, Double> entry : totales.entrySet())
            if (!entry.getKey().equals("Efectivo") && !entry.getKey().contains("Cuenta Corriente"))
                digital += entry.getValue();

        double totalFacturado = efectivo + digital + fiadas;
        double totalCajon    = fondo + efectivo + pagosFiados;

        StringBuilder msg = new StringBuilder("💰 RESUMEN DEL TURNO\n");
        msg.append("══════════════════════════════\n\n");
        msg.append("💵 EFECTIVO EN CAJÓN:\n");
        msg.append("  • Fondo inicial: $").append(String.format("%.2f", fondo)).append("\n");
        msg.append("  • Ventas en efectivo: $").append(String.format("%.2f", efectivo)).append("\n");
        if (pagosFiados > 0) msg.append("  • Cobro de fiados: $").append(String.format("%.2f", pagosFiados)).append("\n");
        msg.append("  ➜ EN CAJÓN FÍSICO: $").append(String.format("%.2f", totalCajon)).append("\n\n");

        if (digital > 0) {
            msg.append("📱 MÉTODOS DIGITALES:\n");
            for (Map.Entry<String, Double> e : totales.entrySet())
                if (!e.getKey().equals("Efectivo") && !e.getKey().contains("Cuenta Corriente"))
                    msg.append("  • ").append(e.getKey()).append(": $").append(String.format("%.2f", e.getValue())).append("\n");
            msg.append("\n");
        }
        if (fiadas > 0) msg.append("📒 Ventas a crédito (fiado): $").append(String.format("%.2f", fiadas)).append("\n\n");

        msg.append("══════════════════════════════\n");
        msg.append("TOTAL FACTURADO: $").append(String.format("%.2f", totalFacturado)).append("\n");
        msg.append("══════════════════════════════\n\n¿Cerrar la caja ahora?");

        if (JOptionPane.showConfirmDialog(this, msg.toString(), "Arqueo de Caja",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION) {
            com.tienda.db.VentaDAO.registrarCierre(nombreUsuario, fondo, efectivo, pagosFiados, digital, fiadas, totalFacturado);
            com.tienda.db.VentaDAO.cerrarCaja();
            com.tienda.db.ClienteDAO.cerrarPagosCaja();
            JOptionPane.showMessageDialog(this, "✅ Caja cerrada exitosamente. ¡Hasta luego!");
            dispose();
            new VentanaLogin().setVisible(true);
        }
    }
}