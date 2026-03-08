package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import com.tienda.Modelo.Producto; 
import com.tienda.db.ProductoDAO;
import com.tienda.db.UsuarioDAO;
import com.tienda.db.ClienteDAO; 
import java.util.ArrayList;
import java.util.List;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;

public class VentanaProducto extends JFrame {
    
    private JTabbedPane pestañas;
    
    private JTextField txtCodigoBarras, txtNombre, txtCosto, txtStock, txtTipo, txtMarca;
    private JComboBox<String> comboMargen;
    
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    private TableRowSorter<DefaultTableModel> sorterInventario;
    
    private DefaultTableModel modeloVenta;
    private JTable tablaVenta; 
    private JLabel lblTotal;
    private double totalVenta = 0;
    private JTextField txtPagaCon;
    private JLabel lblVuelto;
    
    private JComboBox<String> cbMetodoPago; 
    private JComboBox<String> cbClienteVenta; // NUEVO: Selector de cliente en la caja
    private JTextField txtBusquedaManual; 
    private DefaultTableModel modeloHistorial;
    
    private DefaultTableModel modeloClientes;
    private JTable tablaClientes;
    
    private String rolActual; 
    private String nombreUsuario; 
    
    private List<Producto> cacheProductos = new ArrayList<>();
    private List<String> cacheTipos = new ArrayList<>();
    private List<String> cacheMarcas = new ArrayList<>();

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
        try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch (Exception ex) {}

        setTitle("Sistema POS - Usuario: " + nombreUsuario + " (" + rolActual + ")");
        setSize(1000, 700); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        pestañas = new JTabbedPane();

        if (rolActual.equals("ADMIN")) {
            crearPestañaDashboard(); 
            crearPestañaRegistro();
            crearPestañaInventario();
            crearPestañaVentas();
            crearPestañaClientes(); 
            crearPestañaHistorial(); 
            crearPestañaUsuarios();
        } else if (rolActual.equals("CAJERO")) {
            crearPestañaVentas();
            crearPestañaClientes(); 
            crearPestañaInventario(); 
        }

        JPanel panelTop = new JPanel(new BorderLayout());
        panelTop.setBorder(new EmptyBorder(10, 15, 10, 15));

        JButton btnCerrarSesion = new JButton("🚪 Cerrar Sesión");
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarSesion.setBackground(new Color(231, 76, 60)); 
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.addActionListener(e -> {
            this.dispose(); 
            new VentanaLogin().setVisible(true); 
        });

        JToggleButton btnTema = new JToggleButton("🌙 Modo Oscuro");
        btnTema.setFocusPainted(false);
        btnTema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTema.addActionListener(e -> aplicarTema(btnTema.isSelected(), btnTema));

        JPanel panelBotonesIzquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        panelBotonesIzquierda.add(btnCerrarSesion);
        panelBotonesIzquierda.add(btnTema);

        JLabel lblReloj = new JLabel();
        lblReloj.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblReloj.setHorizontalAlignment(SwingConstants.RIGHT);
        
        Timer timer = new Timer(1000, e -> {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy  |  HH:mm:ss");
            lblReloj.setText("📅 " + sdf.format(new java.util.Date()));
        });
        timer.start();

        panelTop.add(panelBotonesIzquierda, BorderLayout.WEST);
        panelTop.add(lblReloj, BorderLayout.EAST);

        add(panelTop, BorderLayout.NORTH);
        add(pestañas, BorderLayout.CENTER);
        
        SwingUtilities.updateComponentTreeUI(this);
    }
    
    private void recargarCaches() {
        cacheProductos = ProductoDAO.obtenerTodos();
        cacheTipos = ProductoDAO.obtenerTipos();
        cacheMarcas = ProductoDAO.obtenerMarcas();
    }

    private void aplicarTema(boolean oscuro, JToggleButton btn) {
        try {
            if (oscuro) { UIManager.setLookAndFeel(new FlatDarkLaf()); btn.setText("☀️ Modo Claro"); } 
            else { UIManager.setLookAndFeel(new FlatLightLaf()); btn.setText("🌙 Modo Oscuro"); }
            FlatLaf.updateUI(); SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {}
    }

    // ==============================================================
    // CUENTAS CORRIENTES (CON EDICIÓN Y ELIMINACIÓN)
    // ==============================================================
    private void crearPestañaClientes() {
        JPanel panelClientes = new JPanel(new BorderLayout(10, 10));
        panelClientes.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panelArriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelArriba.setBorder(BorderFactory.createTitledBorder("Registrar Nuevo Cliente en la Libreta"));
        
        JTextField txtNomCliente = new JTextField(15);
        JTextField txtTelCliente = new JTextField(12);
        JButton btnGuardarCliente = new JButton("➕ Agregar Cliente");
        btnGuardarCliente.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        panelArriba.add(new JLabel("Nombre Completo:")); panelArriba.add(txtNomCliente);
        panelArriba.add(new JLabel("Teléfono:")); panelArriba.add(txtTelCliente);
        panelArriba.add(btnGuardarCliente);

        String[] cols = {"ID", "NOMBRE DEL CLIENTE", "TELÉFONO", "DEUDA TOTAL"};
        modeloClientes = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaClientes = new JTable(modeloClientes);
        aplicarEstiloTabla(tablaClientes);

        Runnable actualizarTablaClientes = () -> {
            modeloClientes.setRowCount(0);
            for (String[] c : ClienteDAO.obtenerClientes()) {
                double deuda = Double.parseDouble(c[3].replace(",", "."));
                modeloClientes.addRow(new Object[]{c[0], c[1], c[2], "$" + String.format("%.2f", deuda)});
            }
            cargarComboClientesCaja(); // Mantiene la caja registradora sincronizada
        };
        actualizarTablaClientes.run();

        tablaClientes.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (column == 3) {
                    try {
                        if (Double.parseDouble(value.toString().replace("$", "").replace(",", ".")) > 0 && !isSelected) {
                            c.setForeground(new Color(231, 76, 60)); c.setFont(c.getFont().deriveFont(Font.BOLD));
                        } else if (!isSelected) c.setForeground(table.getForeground());
                    } catch(Exception e){}
                } else if (!isSelected) c.setForeground(table.getForeground());
                return c;
            }
        });

        // NUEVO: Menú Clic Derecho en Clientes
        JPopupMenu menuClientes = new JPopupMenu();
        JMenuItem itemEditarCli = new JMenuItem("Editar Cliente");
        JMenuItem itemEliminarCli = new JMenuItem("Eliminar Cliente");
        menuClientes.add(itemEditarCli);
        menuClientes.add(itemEliminarCli);
        tablaClientes.setComponentPopupMenu(menuClientes);

        itemEditarCli.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if(row != -1) {
                int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
                String nombreActual = modeloClientes.getValueAt(row, 1).toString();
                String telActual = modeloClientes.getValueAt(row, 2).toString();

                JTextField txtN = new JTextField(nombreActual);
                JTextField txtT = new JTextField(telActual);
                Object[] message = { "Nombre:", txtN, "Teléfono:", txtT };

                int option = JOptionPane.showConfirmDialog(this, message, "Editar Cliente", JOptionPane.OK_CANCEL_OPTION);
                if (option == JOptionPane.OK_OPTION) {
                    if(ClienteDAO.actualizarCliente(id, txtN.getText().trim(), txtT.getText().trim())){
                        actualizarTablaClientes.run();
                        JOptionPane.showMessageDialog(this, "Cliente actualizado.");
                    } else {
                        JOptionPane.showMessageDialog(this, "El teléfono ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        itemEliminarCli.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if(row != -1) {
                double deuda = Double.parseDouble(modeloClientes.getValueAt(row, 3).toString().replace("$","").replace(",","."));
                if(deuda > 0) {
                    JOptionPane.showMessageDialog(this, "No puedes eliminar un cliente que te debe dinero. Saldá su deuda primero.", "Acción Denegada", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
                if (JOptionPane.showConfirmDialog(this, "¿Eliminar este cliente y todo su historial?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    ClienteDAO.eliminarCliente(id);
                    actualizarTablaClientes.run();
                }
            }
        });

        JPanel panelAbajo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnVerHistorial = new JButton("📄 Ver Movimientos / Detalles");
        JButton btnSaldar = new JButton("💵 Ingresar Abono de Deuda");
        btnVerHistorial.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaldar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSaldar.setBackground(new Color(46, 204, 113)); btnSaldar.setForeground(Color.WHITE);
        panelAbajo.add(btnVerHistorial); panelAbajo.add(btnSaldar);

        btnGuardarCliente.addActionListener(e -> {
            String nom = txtNomCliente.getText().trim();
            if(nom.isEmpty()) { JOptionPane.showMessageDialog(this, "El nombre es obligatorio"); return; }
            if(ClienteDAO.registrarCliente(nom, txtTelCliente.getText().trim())) {
                txtNomCliente.setText(""); txtTelCliente.setText("");
                actualizarTablaClientes.run();
                JOptionPane.showMessageDialog(this, "Cliente registrado");
            } else {
                JOptionPane.showMessageDialog(this, "El teléfono ingresado ya está registrado por otro cliente.", "Teléfono Duplicado", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnSaldar.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Seleccione un cliente primero"); return; }
            int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
            String nom = modeloClientes.getValueAt(row, 1).toString();
            String abonoStr = JOptionPane.showInputDialog(this, "¿Abono de " + nom + "?", "Registrar Pago", JOptionPane.QUESTION_MESSAGE);
            if (abonoStr != null && !abonoStr.trim().isEmpty()) {
                try {
                    double monto = Double.parseDouble(abonoStr.replace(",", "."));
                    ClienteDAO.registrarMovimiento(id, "PAGO", monto, "Abono de deuda (" + nombreUsuario + ")");
                    actualizarTablaClientes.run();
                } catch(Exception ex) { JOptionPane.showMessageDialog(this, "Monto inválido"); }
            }
        });

        btnVerHistorial.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Seleccione un cliente primero"); return; }
            int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
            String nom = modeloClientes.getValueAt(row, 1).toString();
            JDialog diag = new JDialog(this, "Libreta de " + nom, true); diag.setSize(650, 450); diag.setLocationRelativeTo(this);
            String[] colHist = {"FECHA", "TIPO", "MONTO", "DETALLE DE PRODUCTOS/ABONO"};
            DefaultTableModel modHist = new DefaultTableModel(colHist, 0) { public boolean isCellEditable(int r, int c) { return false; } };
            JTable tabHist = new JTable(modHist); aplicarEstiloTabla(tabHist); tabHist.getColumnModel().getColumn(3).setPreferredWidth(250);
            for(String[] mov : ClienteDAO.obtenerHistorialCliente(id)) modHist.addRow(mov);
            tabHist.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
                public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                    Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    String tipo = table.getValueAt(row, 1).toString();
                    if (!isSelected) {
                        if(tipo.equals("PAGO")) c.setForeground(new Color(46, 204, 113));
                        else if (tipo.equals("FIADO")) c.setForeground(new Color(231, 76, 60));
                        else c.setForeground(table.getForeground());
                    }
                    return c;
                }
            });
            diag.add(new JScrollPane(tabHist)); diag.setVisible(true);
        });

        panelClientes.add(panelArriba, BorderLayout.NORTH);
        panelClientes.add(new JScrollPane(tablaClientes), BorderLayout.CENTER);
        panelClientes.add(panelAbajo, BorderLayout.SOUTH);
        pestañas.addTab("👥 Cuentas Corrientes", panelClientes);
    }

    private void cargarComboClientesCaja() {
        if (cbClienteVenta != null) {
            cbClienteVenta.removeAllItems();
            cbClienteVenta.addItem("0 - Consumidor Final"); // El default
            for (String[] c : ClienteDAO.obtenerClientes()) {
                cbClienteVenta.addItem(c[0] + " - " + c[1]);
            }
        }
    }

    // ==============================================================
    // GESTIÓN DE USUARIOS
    // ==============================================================
    private void crearPestañaUsuarios() {
        JPanel panelUsuarios = new JPanel(new BorderLayout(15, 15));
        panelUsuarios.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel panelForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelForm.setBorder(BorderFactory.createTitledBorder("Crear Nueva Cuenta de Empleado"));

        JTextField txtUsuario = new JTextField(12);
        JPasswordField txtPass = new JPasswordField(12);
        JComboBox<String> cbRol = new JComboBox<>(new String[]{"CAJERO", "ADMIN"});
        JButton btnGuardarUser = new JButton("💾 Guardar Usuario");
        btnGuardarUser.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelForm.add(new JLabel("Usuario:")); panelForm.add(txtUsuario);
        panelForm.add(new JLabel("Contraseña:")); panelForm.add(txtPass);
        panelForm.add(new JLabel("Rol:")); panelForm.add(cbRol);
        panelForm.add(btnGuardarUser);

        String[] cols = {"NOMBRE DE USUARIO", "NIVEL DE ACCESO (ROL)"};
        DefaultTableModel modUsers = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tablaUsers = new JTable(modUsers); aplicarEstiloTabla(tablaUsers);
        
        Runnable refrescarTablaUsuarios = () -> { modUsers.setRowCount(0); for (String[] u : UsuarioDAO.obtenerTodos()) modUsers.addRow(u); };
        refrescarTablaUsuarios.run();

        btnGuardarUser.addActionListener(e -> {
            String u = txtUsuario.getText().trim(); String p = new String(txtPass.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "El usuario y contraseña son obligatorios"); return; }
            if (UsuarioDAO.registrarUsuario(u, p, cbRol.getSelectedItem().toString())) {
                JOptionPane.showMessageDialog(this, "¡Cuenta creada exitosamente!");
                txtUsuario.setText(""); txtPass.setText(""); refrescarTablaUsuarios.run();
            } else JOptionPane.showMessageDialog(this, "El nombre de usuario ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
        });

        JPopupMenu pop = new JPopupMenu(); JMenuItem itemDel = new JMenuItem("Eliminar Cuenta"); pop.add(itemDel); tablaUsers.setComponentPopupMenu(pop);
        itemDel.addActionListener(e -> {
            int row = tablaUsers.getSelectedRow();
            if (row != -1) {
                String userToDel = modUsers.getValueAt(row, 0).toString();
                if (userToDel.equalsIgnoreCase(nombreUsuario) || userToDel.equalsIgnoreCase("admin")) return; 
                if (JOptionPane.showConfirmDialog(this, "¿Borrar cuenta?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    UsuarioDAO.eliminarUsuario(userToDel); refrescarTablaUsuarios.run();
                }
            }
        });

        panelUsuarios.add(panelForm, BorderLayout.NORTH); panelUsuarios.add(new JScrollPane(tablaUsers), BorderLayout.CENTER); pestañas.addTab("🛡️ Gestión Empleados", panelUsuarios);
    }

    private void crearPestañaDashboard() {
        JPanel panelDashboard = new JPanel(new BorderLayout(20, 20)); panelDashboard.setBorder(new EmptyBorder(30, 30, 30, 30));
        JLabel lblTitulo = new JLabel("Resumen del Negocio", SwingConstants.CENTER); lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 28)); panelDashboard.add(lblTitulo, BorderLayout.NORTH);
        JPanel panelTarjetas = new JPanel(new GridLayout(1, 3, 20, 0));

        JPanel tarjetaVentas = crearTarjeta("Ingresos Totales", "$" + String.format("%.2f", com.tienda.db.VentaDAO.obtenerTotalVentasHistorico()), new Color(46, 204, 113)); 
        JPanel tarjetaProductos = crearTarjeta("Productos Catálogo", String.valueOf(ProductoDAO.obtenerTotalProductosRegistrados()), new Color(52, 152, 219)); 
        JPanel tarjetaAlertas = crearTarjeta("Alertas Bajo Stock", String.valueOf(ProductoDAO.obtenerProductosBajoStock(5)), new Color(231, 76, 60)); 

        tarjetaAlertas.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tarjetaAlertas.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                pestañas.setSelectedIndex(2); 
                if (sorterInventario != null) {
                    sorterInventario.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                            try { return Integer.parseInt(entry.getModel().getValueAt(entry.getIdentifier(), 6).toString()) <= 5; } catch(Exception ex) { return false; }
                        }
                    });
                }
            }
        });
        panelTarjetas.add(tarjetaVentas); panelTarjetas.add(tarjetaProductos); panelTarjetas.add(tarjetaAlertas);
        panelDashboard.add(panelTarjetas, BorderLayout.CENTER);
        
        JButton btnRefrescar = new JButton("↻ Actualizar Estadísticas"); btnRefrescar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRefrescar.addActionListener(e -> { pestañas.remove(panelDashboard); crearPestañaDashboard(); pestañas.setSelectedIndex(0); });
        JPanel panelSur = new JPanel(); panelSur.add(btnRefrescar); panelDashboard.add(panelSur, BorderLayout.SOUTH);
        pestañas.insertTab("📊 Dashboard", null, panelDashboard, "Resumen General", 0);
    }

    private JPanel crearTarjeta(String titulo, String valor, Color colorBorde) {
        JPanel tarjeta = new JPanel(new GridLayout(2, 1));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(colorBorde, 4, true), new EmptyBorder(20, 20, 20, 20)));
        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER); lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16)); lblTitulo.setForeground(Color.GRAY);
        JLabel lblValor = new JLabel(valor, SwingConstants.CENTER); lblValor.setFont(new Font("SansSerif", Font.BOLD, 36));
        tarjeta.add(lblTitulo); tarjeta.add(lblValor); return tarjeta;
    }

    private JPanel crearBuscadorConDesplegable(JTextField textField, java.util.function.Supplier<List<String>> proveedor, boolean isVentas) {
        JPanel panel = new JPanel(new BorderLayout()); panel.add(textField, BorderLayout.CENTER);
        JButton btnDrop = new JButton("▼"); btnDrop.setPreferredSize(new Dimension(35, 0)); btnDrop.setFocusable(false); btnDrop.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(btnDrop, BorderLayout.EAST);
        JPopupMenu popup = new JPopupMenu(); popup.setFocusable(false);
        
        java.util.function.Consumer<String> mostrarPopup = (filtro) -> {
            popup.removeAll(); String f = filtro.toLowerCase().trim(); int count = 0;
            for (String op : proveedor.get()) {
                if (f.isEmpty() || op.toLowerCase().contains(f)) {
                    JMenuItem item = new JMenuItem(op); item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    item.addMouseListener(new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                            if (isVentas) { textField.setText(""); popup.setVisible(false); procesarSeleccionVenta(op); } 
                            else { textField.setText(op); popup.setVisible(false); }
                            textField.requestFocus();
                        }
                    });
                    popup.add(item); count++; if (count >= 15) break; 
                }
            }
            if (count > 0) { popup.show(textField, 0, textField.getHeight()); textField.requestFocus(); } else popup.setVisible(false);
        };

        textField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_ESCAPE) { popup.setVisible(false); return; }
                if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_UP || c == KeyEvent.VK_DOWN || c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT) return;
                mostrarPopup.accept(textField.getText());
            }
        });

        btnDrop.addActionListener(e -> { textField.requestFocus(); mostrarPopup.accept(""); });
        textField.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> textField.selectAll()); } });
        
        if (isVentas) {
            textField.addActionListener(e -> {
                String text = textField.getText().trim().toLowerCase(); if (text.isEmpty()) return;
                for (String op : proveedor.get()) {
                    if (op.toLowerCase().contains(text)) { procesarSeleccionVenta(op); textField.setText(""); popup.setVisible(false); return; }
                }
            });
        }
        return panel;
    }

    private List<String> obtenerListaProductosFormateada() {
        List<String> lista = new ArrayList<>();
        for (Producto p : cacheProductos) lista.add(p.getCodigoBarras() + " - " + p.getTipo() + " " + p.getMarca() + " " + p.getNombre());
        return lista;
    }

    private void procesarSeleccionVenta(String seleccion) {
        if(seleccion.contains(" - ")) {
            String codigo = seleccion.split(" - ")[0];
            Producto p = ProductoDAO.buscarPorCodigo(codigo);
            if (p != null && p.getStock() > 0) agregarAlCarrito(p); else JOptionPane.showMessageDialog(this, "Sin stock disponible");
        }
    }

    private void crearPestañaRegistro() {
        JPanel panelRegistro = new JPanel(new BorderLayout(10, 10)); panelRegistro.setBorder(new EmptyBorder(15, 80, 15, 80)); 
        JPanel form = new JPanel(new GridLayout(7, 2, 10, 15)); form.setBorder(BorderFactory.createTitledBorder("Datos del Producto"));
        Font f = new Font("SansSerif", Font.PLAIN, 15);

        txtCodigoBarras = new JTextField(); txtCodigoBarras.setFont(f); txtNombre = new JTextField(); txtNombre.setFont(f);
        txtTipo = new JTextField(); txtTipo.setFont(f); JPanel panelTipo = crearBuscadorConDesplegable(txtTipo, () -> cacheTipos, false);
        txtMarca = new JTextField(); txtMarca.setFont(f); JPanel panelMarca = crearBuscadorConDesplegable(txtMarca, () -> cacheMarcas, false);
        txtCosto = new JTextField(); txtCosto.setFont(f); txtStock = new JTextField(); txtStock.setFont(f);
        comboMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"}); comboMargen.setFont(f);

        form.add(new JLabel("Código de Barras:") {{ setFont(f); }}); form.add(txtCodigoBarras);
        form.add(new JLabel("Nombre:") {{ setFont(f); }}); form.add(txtNombre);
        form.add(new JLabel("Tipo (Ej: Cargador, Cable):") {{ setFont(f); }}); form.add(panelTipo);
        form.add(new JLabel("Marca (Ej: Samsung, Generico):") {{ setFont(f); }}); form.add(panelMarca);
        form.add(new JLabel("Precio de Costo ($):") {{ setFont(f); }}); form.add(txtCosto);
        form.add(new JLabel("Cantidad en Stock:") {{ setFont(f); }}); form.add(txtStock);
        form.add(new JLabel("Margen Ganancia (%):") {{ setFont(f); }}); form.add(comboMargen);

        JButton btnGuardar = new JButton("GUARDAR PRODUCTO"); btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14)); btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> ejecutarGuardado());

        txtCodigoBarras.addActionListener(e -> txtNombre.requestFocus()); txtNombre.addActionListener(e -> txtTipo.requestFocus());
        txtTipo.addActionListener(e -> txtMarca.requestFocus()); txtMarca.addActionListener(e -> txtCosto.requestFocus());
        txtCosto.addActionListener(e -> txtStock.requestFocus()); txtStock.addActionListener(e -> comboMargen.requestFocus());
        comboMargen.addKeyListener(new KeyAdapter() { public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) btnGuardar.doClick(); } });

        panelRegistro.add(form, BorderLayout.CENTER); panelRegistro.add(btnGuardar, BorderLayout.SOUTH); pestañas.addTab(" Registrar Nuevo", panelRegistro);
    }

    private void aplicarEstiloTabla(JTable t) { t.setRowHeight(30); t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13)); }

    private void crearPestañaInventario() {
        JPanel panelInventario = new JPanel(new BorderLayout(10, 10)); panelInventario.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 5)); panelBusqueda.add(new JLabel("🔍 Buscar producto: "), BorderLayout.WEST);
        JTextField txtBuscador = new JTextField(); panelBusqueda.add(txtBuscador, BorderLayout.CENTER);
        JButton btnActualizarInv = new JButton("↻ Actualizar"); btnActualizarInv.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActualizarInv.addActionListener(e -> { txtBuscador.setText(""); if (sorterInventario != null) sorterInventario.setRowFilter(null); actualizarTabla(); });
        panelBusqueda.add(btnActualizarInv, BorderLayout.EAST); panelInventario.add(panelBusqueda, BorderLayout.NORTH);

        String[] columnas = {"CÓDIGO", "NOMBRE", "TIPO", "MARCA", "COSTO", "VENTA", "STOCK"};
        modeloTabla = new DefaultTableModel(columnas, 0) { public boolean isCellEditable(int r, int c) { return false; } public Class<?> getColumnClass(int c) { return String.class; } };
        tabla = new JTable(modeloTabla); aplicarEstiloTabla(tabla); 
        
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int stock = Integer.parseInt(table.getValueAt(row, 6).toString()); 
                    if (stock <= 5 && !isSelected) { c.setBackground(new Color(255, 204, 204)); c.setForeground(new Color(153, 0, 0)); } 
                    else if (!isSelected) { c.setBackground(table.getBackground()); c.setForeground(table.getForeground()); }
                } catch (Exception e) {} return c;
            }
        });

        sorterInventario = new TableRowSorter<>(modeloTabla); tabla.setRowSorter(sorterInventario); actualizarTabla();
        txtBuscador.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { String text = txtBuscador.getText(); if (text.trim().length() == 0) sorterInventario.setRowFilter(null); else sorterInventario.setRowFilter(RowFilter.regexFilter("(?i)" + text)); } });

        if ("ADMIN".equals(rolActual)) {
            JPopupMenu popupMenu = new JPopupMenu(); JMenuItem itemEditar = new JMenuItem("Editar Producto"); JMenuItem itemEliminar = new JMenuItem("Eliminar Producto(s)");
            popupMenu.add(itemEditar); popupMenu.add(itemEliminar); tabla.setComponentPopupMenu(popupMenu);
            itemEliminar.addActionListener(e -> {
                int[] filasVisuales = tabla.getSelectedRows(); 
                if (filasVisuales.length > 0 && JOptionPane.showConfirmDialog(this, "¿Seguro de borrar?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    for (int i = 0; i < filasVisuales.length; i++) ProductoDAO.eliminarProducto(modeloTabla.getValueAt(tabla.convertRowIndexToModel(filasVisuales[i]), 0).toString());
                    recargarCaches(); actualizarTabla(); JOptionPane.showMessageDialog(this, "Productos eliminados.");
                }
            });
            itemEditar.addActionListener(e -> abrirEdicionDesdeTabla());
            tabla.addMouseListener(new MouseAdapter() { public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) abrirEdicionDesdeTabla(); } });
        }
        panelInventario.add(new JScrollPane(tabla), BorderLayout.CENTER); pestañas.addTab(" Ver Inventario", panelInventario);
    }

    private void abrirEdicionDesdeTabla() {
        int filaVisual = tabla.getSelectedRow();
        if (filaVisual != -1) abrirDialogoEdicion(tabla.convertRowIndexToModel(filaVisual));
    }

    private void abrirDialogoEdicion(int fila) {
        String codigo = tabla.getValueAt(fila, 0).toString(), nAct = tabla.getValueAt(fila, 1).toString(), tAct = tabla.getValueAt(fila, 2).toString(), mAct = tabla.getValueAt(fila, 3).toString();
        String cAct = tabla.getValueAt(fila, 4).toString(), sAct = tabla.getValueAt(fila, 6).toString(); 
        JDialog dialogo = new JDialog(this, "Editar Producto", true); dialogo.setLayout(new GridLayout(7, 2, 10, 10)); dialogo.setSize(400, 350); dialogo.setLocationRelativeTo(this);
        JTextField txtNom = new JTextField(nAct), txtTip = new JTextField(tAct.equals("-") ? "" : tAct), txtMar = new JTextField(mAct.equals("-") ? "" : mAct); 
        JPanel panelTip = crearBuscadorConDesplegable(txtTip, () -> cacheTipos, false), panelMar = crearBuscadorConDesplegable(txtMar, () -> cacheMarcas, false);
        JTextField txtCos = new JTextField(cAct), txtStk = new JTextField(sAct);
        JComboBox<String> cbMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});
        dialogo.add(new JLabel(" Nombre:")); dialogo.add(txtNom); dialogo.add(new JLabel(" Tipo:")); dialogo.add(panelTip);
        dialogo.add(new JLabel(" Marca:")); dialogo.add(panelMar); dialogo.add(new JLabel(" Costo:")); dialogo.add(txtCos);
        dialogo.add(new JLabel(" Stock:")); dialogo.add(txtStk); dialogo.add(new JLabel(" Nuevo Margen %:")); dialogo.add(cbMargen);
        JButton btnConfirmar = new JButton("Guardar Cambios");
        btnConfirmar.addActionListener(ev -> {
            try {
                Producto pEditado = new Producto(codigo, txtNom.getText(), txtTip.getText().trim().isEmpty() ? "-" : txtTip.getText().trim(), txtMar.getText().trim().isEmpty() ? "-" : txtMar.getText().trim(), 
                    Double.parseDouble(txtCos.getText()), Integer.parseInt(txtStk.getText()), Double.parseDouble(cbMargen.getSelectedItem().toString()));
                ProductoDAO.actualizarProducto(pEditado); recargarCaches(); actualizarTabla(); dialogo.dispose(); JOptionPane.showMessageDialog(this, "¡Producto actualizado!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(dialogo, "Datos inválidos"); }
        });
        dialogo.add(new JLabel("")); dialogo.add(btnConfirmar); dialogo.setVisible(true);
    }

    // ==============================================================
    // PESTAÑA VENTAS Y CAJA (CON SELECTOR DE CLIENTE)
    // ==============================================================
    private void crearPestañaVentas() {
        JPanel panelVentas = new JPanel(new BorderLayout(10, 10)); panelVentas.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel panelArriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        
        JTextField txtEscaneo = new JTextField(15); txtEscaneo.setBorder(BorderFactory.createTitledBorder("Escanear Código")); 
        txtEscaneo.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtEscaneo.selectAll()); }});
        
        txtBusquedaManual = new JTextField(25); JPanel panelBusqueda = crearBuscadorConDesplegable(txtBusquedaManual, () -> obtenerListaProductosFormateada(), true);
        panelBusqueda.setBorder(BorderFactory.createTitledBorder("Buscar Manualmente"));
        panelArriba.add(txtEscaneo); panelArriba.add(panelBusqueda);
        
        modeloVenta = new DefaultTableModel(new String[]{"CÓDIGO", "PRODUCTO", "PRECIO", "CANT.", "SUBTOTAL"}, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        tablaVenta = new JTable(modeloVenta); aplicarEstiloTabla(tablaVenta); 

        JPanel panelControlesCarrito = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSumar = new JButton("+ Sumar 1"), btnRestar = new JButton("- Restar 1"), btnQuitar = new JButton("Quitar Todo (X)");
        btnSumar.addActionListener(e -> ajustarCantidadCarrito(1)); btnRestar.addActionListener(e -> ajustarCantidadCarrito(-1)); btnQuitar.addActionListener(e -> ajustarCantidadCarrito(0));
        panelControlesCarrito.add(new JLabel("Seleccione en la tabla y ajuste: ")); panelControlesCarrito.add(btnSumar); panelControlesCarrito.add(btnRestar); panelControlesCarrito.add(btnQuitar);
        JPanel panelCentro = new JPanel(new BorderLayout()); panelCentro.add(new JScrollPane(tablaVenta), BorderLayout.CENTER); panelCentro.add(panelControlesCarrito, BorderLayout.SOUTH);

        // AQUÍ SE AGREGA EL SELECTOR DE CLIENTE
        JPanel panelAbajo = new JPanel(new GridLayout(2, 4, 15, 10)); panelAbajo.setBorder(BorderFactory.createTitledBorder("Detalles de Pago y Cliente"));
        
        cbClienteVenta = new JComboBox<>();
        cargarComboClientesCaja(); // Carga al consumidor final y a la libreta
        
        cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito", "Cuenta Corriente (Fiado)"});
        txtPagaCon = new JTextField(); 
        txtPagaCon.addFocusListener(new FocusAdapter() { public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtPagaCon.selectAll()); }});
        lblVuelto = new JLabel("Vuelto: $0.00"); lblVuelto.setFont(new Font("SansSerif", Font.BOLD, 18)); 

        panelAbajo.add(new JLabel("Cliente:")); 
        panelAbajo.add(new JLabel("Método de Pago:")); 
        panelAbajo.add(new JLabel("El cliente abona con ($):")); 
        panelAbajo.add(new JLabel("")); 
        
        panelAbajo.add(cbClienteVenta); 
        panelAbajo.add(cbMetodoPago); 
        panelAbajo.add(txtPagaCon); 
        panelAbajo.add(lblVuelto);
        
        JPanel panelSur = new JPanel(new BorderLayout()); panelSur.add(panelAbajo, BorderLayout.CENTER);
        JPanel panelCobrar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        lblTotal = new JLabel("TOTAL: $0.00"); lblTotal.setFont(new Font("SansSerif", Font.BOLD, 22));
        JButton btnFinalizar = new JButton("CONFIRMAR VENTA"); btnFinalizar.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelCobrar.add(lblTotal); panelCobrar.add(btnFinalizar); panelSur.add(panelCobrar, BorderLayout.SOUTH);

        txtEscaneo.addActionListener(e -> { Producto p = ProductoDAO.buscarPorCodigo(txtEscaneo.getText()); if (p != null && p.getStock() > 0) { agregarAlCarrito(p); txtEscaneo.setText(""); } else JOptionPane.showMessageDialog(this, "Sin stock/No encontrado"); });
        txtPagaCon.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { calcularVuelto(); } });
        btnFinalizar.addActionListener(e -> { if(totalVenta > 0) finalizarVenta(); });

        panelVentas.add(panelArriba, BorderLayout.NORTH); panelVentas.add(panelCentro, BorderLayout.CENTER); panelVentas.add(panelSur, BorderLayout.SOUTH);
        pestañas.addTab("🛒 Caja y Ventas", panelVentas);
    }

    private void agregarAlCarrito(Producto p) {
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            if (modeloVenta.getValueAt(i, 0).equals(p.getCodigoBarras())) {
                int cantAct = (int) modeloVenta.getValueAt(i, 3);
                if (cantAct >= p.getStock()) { JOptionPane.showMessageDialog(this, "No hay más stock disponible"); return; }
                modeloVenta.setValueAt(cantAct + 1, i, 3); modeloVenta.setValueAt((cantAct + 1) * p.getPrecioVenta(), i, 4); totalVenta += p.getPrecioVenta(); lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta)); calcularVuelto(); return;
            }
        }
        modeloVenta.addRow(new Object[]{p.getCodigoBarras(), p.getNombre(), p.getPrecioVenta(), 1, p.getPrecioVenta()});
        totalVenta += p.getPrecioVenta(); lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta)); calcularVuelto(); 
    }

    // FINALIZAR VENTA ACTUALIZADO
    private void finalizarVenta() {
        String metodo = cbMetodoPago.getSelectedItem().toString();
        String clienteSelect = cbClienteVenta.getSelectedItem().toString(); // ej: "0 - Consumidor Final"
        
        int clienteId = Integer.parseInt(clienteSelect.split(" - ")[0]);
        String nombreClienteFiado = clienteSelect.split(" - ")[1];

        // Validar lógica de fiado
        if (metodo.equals("Cuenta Corriente (Fiado)") && clienteId == 0) {
            JOptionPane.showMessageDialog(this, "Para fiar, debes seleccionar un Cliente registrado de la lista, no al Consumidor Final.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        StringBuilder resumen = new StringBuilder();
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            String cod = modeloVenta.getValueAt(i, 0).toString(); int cant = (int) modeloVenta.getValueAt(i, 3);
            ProductoDAO.reducirStock(cod, cant); resumen.append(cant).append("x ").append(modeloVenta.getValueAt(i, 1)).append(" | ");
        }
        
        // Registrar deuda si es fiado
        if (metodo.equals("Cuenta Corriente (Fiado)")) {
            ClienteDAO.registrarMovimiento(clienteId, "FIADO", totalVenta, resumen.toString() + " (Vendido por: " + nombreUsuario + ")");
        }
        
        // Registrar en historial general (AHORA ENVÍA EL NOMBRE DEL CLIENTE)
        com.tienda.db.VentaDAO.registrarVenta(resumen.toString(), totalVenta, metodo, nombreUsuario, nombreClienteFiado);
        
        JOptionPane.showMessageDialog(this, "Venta realizada con éxito");
        
        modeloVenta.setRowCount(0); totalVenta = 0; lblTotal.setText("TOTAL: $0.00"); txtPagaCon.setText(""); lblVuelto.setText("Vuelto: $0.00"); 
        cbMetodoPago.setSelectedIndex(0); cbClienteVenta.setSelectedIndex(0);
        
        recargarCaches(); actualizarTabla(); 
        
        // Refrescar tabla de fiados si está abierta
        if (modeloClientes != null) {
            modeloClientes.setRowCount(0);
            for (String[] c : ClienteDAO.obtenerClientes()) modeloClientes.addRow(new Object[]{c[0], c[1], c[2], "$" + String.format("%.2f", Double.parseDouble(c[3].replace(",",".")))});
        }
        
        if ("ADMIN".equals(rolActual)) actualizarHistorial(); 
    }

    private void ejecutarGuardado() {
        try {
            String cod = txtCodigoBarras.getText().trim(), nom = txtNombre.getText().trim();
            if (cod.isEmpty() || nom.isEmpty()) { JOptionPane.showMessageDialog(this, "Código y nombre obligatorios.", "Aviso", JOptionPane.WARNING_MESSAGE); return; }
            if (ProductoDAO.buscarPorCodigo(cod) != null) { JOptionPane.showMessageDialog(this, "Código Duplicado", "Error", JOptionPane.ERROR_MESSAGE); return; }
            String t = txtTipo.getText().trim(), m = txtMarca.getText().trim();
            ProductoDAO.registrarProducto(new Producto(cod, nom, t.isEmpty() ? "-" : t, m.isEmpty() ? "-" : m, Double.parseDouble(txtCosto.getText()), Integer.parseInt(txtStock.getText()), Double.parseDouble(comboMargen.getSelectedItem().toString())));
            JOptionPane.showMessageDialog(this, "Producto guardado"); recargarCaches(); actualizarTabla(); 
            txtCodigoBarras.setText(""); txtNombre.setText(""); txtCosto.setText(""); txtStock.setText(""); txtTipo.setText(""); txtMarca.setText(""); txtCodigoBarras.requestFocus();
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Datos numéricos incorrectos."); }
    }

    public void actualizarTabla() {
        modeloTabla.setRowCount(0); for (Producto p : cacheProductos) modeloTabla.addRow(new Object[]{ p.getCodigoBarras(), p.getNombre(), p.getTipo(), p.getMarca(), p.getPrecioCosto(), p.getPrecioVenta(), p.getStock()});
    }

    // ==============================================================
    // PESTAÑA HISTORIAL (CON COLUMNA CLIENTE)
    // ==============================================================
    private void crearPestañaHistorial() {
        JPanel panelHistorial = new JPanel(new BorderLayout(10, 10)); panelHistorial.setBorder(new EmptyBorder(10, 10, 10, 10));
        JPanel panelArribaHistorial = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnActualizarHistorial = new JButton("↻ Actualizar Historial"); btnActualizarHistorial.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActualizarHistorial.addActionListener(e -> actualizarHistorial()); panelArribaHistorial.add(btnActualizarHistorial);
        panelHistorial.add(panelArribaHistorial, BorderLayout.NORTH);

        // NUEVO: COLUMNA "CLIENTE"
        String[] cols = {"Nº TICKET", "FECHA", "CLIENTE", "DETALLE DE PRODUCTOS", "TOTAL", "MÉTODO", "CAJERO"};
        modeloHistorial = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tablaHistorial = new JTable(modeloHistorial); aplicarEstiloTabla(tablaHistorial); tablaHistorial.getColumnModel().getColumn(3).setPreferredWidth(300);

        actualizarHistorial(); panelHistorial.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER); pestañas.addTab("📋 Historial de Ventas", panelHistorial);
    }

    private void actualizarHistorial() {
        if (modeloHistorial != null) {
            modeloHistorial.setRowCount(0);
            for (String[] fila : com.tienda.db.VentaDAO.obtenerHistorial()) modeloHistorial.addRow(fila);
        }
    }

    private void ajustarCantidadCarrito(int cambio) {
        int[] filas = tablaVenta.getSelectedRows(); if (filas.length == 0) { JOptionPane.showMessageDialog(this, "Seleccione un producto."); return; }
        for (int i = filas.length - 1; i >= 0; i--) {
            int fila = filas[i]; int cantAct = (int) modeloVenta.getValueAt(fila, 3); double precio = (double) modeloVenta.getValueAt(fila, 2);
            if (cambio == 0 || cantAct + cambio <= 0) { totalVenta -= (cantAct * precio); modeloVenta.removeRow(fila); } 
            else {
                if (cambio > 0 && cantAct + cambio > ProductoDAO.buscarPorCodigo(modeloVenta.getValueAt(fila, 0).toString()).getStock()) continue; 
                modeloVenta.setValueAt(cantAct + cambio, fila, 3); modeloVenta.setValueAt((cantAct + cambio) * precio, fila, 4); totalVenta += (cambio * precio);
            }
        }
        lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta)); calcularVuelto(); 
    }

    private void calcularVuelto() {
        try {
            if (txtPagaCon.getText().trim().isEmpty()) { lblVuelto.setText("Vuelto: $0.00"); lblVuelto.setForeground(UIManager.getColor("Label.foreground")); return; }
            double abona = Double.parseDouble(txtPagaCon.getText()), vuelto = abona - totalVenta;
            if (vuelto < 0) { lblVuelto.setText("FALTAN: $" + String.format("%.2f", Math.abs(vuelto))); lblVuelto.setForeground(new Color(231, 76, 60)); } 
            else { lblVuelto.setText("Vuelto: $" + String.format("%.2f", vuelto)); lblVuelto.setForeground(new Color(46, 204, 113)); }
        } catch (Exception ex) { lblVuelto.setText("Vuelto: $0.00"); lblVuelto.setForeground(UIManager.getColor("Label.foreground")); }
    }
}