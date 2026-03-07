package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import com.tienda.Modelo.Producto; 
import com.tienda.db.ProductoDAO;
import java.util.ArrayList;
import java.util.List;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;

public class VentanaProducto extends JFrame {
    
    private JTabbedPane pestañas;
    
    // AHORA SON TEXTFIELDS PUROS, ADIÓS JCOMBOBOX BUGUEADOS
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
    private JTextField txtBusquedaManual; 
    private DefaultTableModel modeloHistorial;
    
    private String rolActual; 
    
    // Memoria RAM
    private List<Producto> cacheProductos = new ArrayList<>();
    private List<String> cacheTipos = new ArrayList<>();
    private List<String> cacheMarcas = new ArrayList<>();

    public VentanaProducto(String rol) {
        this.rolActual = rol;
        recargarCaches(); 

        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("No se pudo cargar FlatLaf");
        }

        setTitle("Sistema de Gestión de Stock y Ventas - Usuario: " + rol);
        setSize(1000, 700); 
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        pestañas = new JTabbedPane();

        if (rol.equals("ADMIN")) {
            crearPestañaDashboard(); 
            crearPestañaRegistro();
            crearPestañaInventario();
            crearPestañaVentas();
            crearPestañaHistorial(); 
        } else if (rol.equals("CAJERO")) {
            crearPestañaVentas();
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
            if (oscuro) {
                UIManager.setLookAndFeel(new FlatDarkLaf());
                btn.setText("☀️ Modo Claro");
            } else {
                UIManager.setLookAndFeel(new FlatLightLaf());
                btn.setText("🌙 Modo Oscuro");
            }
            FlatLaf.updateUI(); 
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception ex) {}
    }

    private void crearPestañaDashboard() {
        JPanel panelDashboard = new JPanel(new BorderLayout(20, 20));
        panelDashboard.setBorder(new EmptyBorder(30, 30, 30, 30));

        JLabel lblTitulo = new JLabel("Resumen del Negocio", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 28));
        panelDashboard.add(lblTitulo, BorderLayout.NORTH);

        JPanel panelTarjetas = new JPanel(new GridLayout(1, 3, 20, 0));

        double totalVendido = com.tienda.db.VentaDAO.obtenerTotalVentasHistorico();
        int totalProductos = ProductoDAO.obtenerTotalProductosRegistrados();
        int productosBajoStock = ProductoDAO.obtenerProductosBajoStock(5); 

        JPanel tarjetaVentas = crearTarjeta("Ingresos Totales", "$" + String.format("%.2f", totalVendido), new Color(46, 204, 113)); 
        JPanel tarjetaProductos = crearTarjeta("Productos Catálogo", String.valueOf(totalProductos), new Color(52, 152, 219)); 
        JPanel tarjetaAlertas = crearTarjeta("Alertas Bajo Stock", String.valueOf(productosBajoStock), new Color(231, 76, 60)); 

        tarjetaAlertas.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tarjetaAlertas.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                pestañas.setSelectedIndex(2); 
                if (sorterInventario != null) {
                    sorterInventario.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
                        @Override
                        public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                            try {
                                int stockActual = Integer.parseInt(entry.getModel().getValueAt(entry.getIdentifier(), 6).toString()); 
                                return stockActual <= 5;
                            } catch(Exception ex) { return false; }
                        }
                    });
                }
            }
        });

        panelTarjetas.add(tarjetaVentas);
        panelTarjetas.add(tarjetaProductos);
        panelTarjetas.add(tarjetaAlertas);

        panelDashboard.add(panelTarjetas, BorderLayout.CENTER);
        
        JButton btnRefrescar = new JButton("↻ Actualizar Estadísticas");
        btnRefrescar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRefrescar.addActionListener(e -> {
            pestañas.remove(panelDashboard);
            crearPestañaDashboard();
            pestañas.setSelectedIndex(0);
        });
        JPanel panelSur = new JPanel();
        panelSur.add(btnRefrescar);
        panelDashboard.add(panelSur, BorderLayout.SOUTH);

        pestañas.insertTab("📊 Dashboard", null, panelDashboard, "Resumen General", 0);
    }

    private JPanel crearTarjeta(String titulo, String valor, Color colorBorde) {
        JPanel tarjeta = new JPanel(new GridLayout(2, 1));
        tarjeta.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(colorBorde, 4, true),
            new EmptyBorder(20, 20, 20, 20)
        ));
        JLabel lblTitulo = new JLabel(titulo, SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblTitulo.setForeground(Color.GRAY);
        JLabel lblValor = new JLabel(valor, SwingConstants.CENTER);
        lblValor.setFont(new Font("SansSerif", Font.BOLD, 36));
        tarjeta.add(lblTitulo); tarjeta.add(lblValor);
        return tarjeta;
    }

    // ==============================================================
    // EL NUEVO BUSCADOR "NIVEL DIOS" (TEXTFIELD + BOTON DESPLEGABLE)
    // ==============================================================
    private JPanel crearBuscadorConDesplegable(JTextField textField, java.util.function.Supplier<List<String>> proveedor, boolean isVentas) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.add(textField, BorderLayout.CENTER);
        
        // El botón de la flechita para ver todo
        JButton btnDrop = new JButton("▼");
        btnDrop.setPreferredSize(new Dimension(35, 0));
        btnDrop.setFocusable(false);
        btnDrop.setCursor(new Cursor(Cursor.HAND_CURSOR));
        panel.add(btnDrop, BorderLayout.EAST);
        
        JPopupMenu popup = new JPopupMenu();
        popup.setFocusable(false);
        
        // Motor de renderizado del menú flotante
        java.util.function.Consumer<String> mostrarPopup = (filtro) -> {
            popup.removeAll();
            String f = filtro.toLowerCase().trim();
            int count = 0;
            for (String op : proveedor.get()) {
                if (f.isEmpty() || op.toLowerCase().contains(f)) {
                    JMenuItem item = new JMenuItem(op);
                    item.setCursor(new Cursor(Cursor.HAND_CURSOR));
                    
                    // ¡AQUÍ ESTÁ LA MAGIA DEL CLIC! Usamos mousePressed para que nunca falle
                    item.addMouseListener(new MouseAdapter() {
                        public void mousePressed(MouseEvent e) {
                            if (isVentas) {
                                textField.setText("");
                                popup.setVisible(false);
                                procesarSeleccionVenta(op); // Agrega al carrito
                            } else {
                                textField.setText(op);
                                popup.setVisible(false);
                            }
                            textField.requestFocus();
                        }
                    });
                    popup.add(item);
                    count++;
                    if (count >= 15) break; // Límite para que no se salga de la pantalla
                }
            }
            if (count > 0) {
                popup.show(textField, 0, textField.getHeight());
                textField.requestFocus();
            } else {
                popup.setVisible(false);
            }
        };

        // Filtro mientras escribes
        textField.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                int c = e.getKeyCode();
                if (c == KeyEvent.VK_ESCAPE) { popup.setVisible(false); return; }
                if (c == KeyEvent.VK_ENTER || c == KeyEvent.VK_UP || c == KeyEvent.VK_DOWN || c == KeyEvent.VK_LEFT || c == KeyEvent.VK_RIGHT) return;
                mostrarPopup.accept(textField.getText());
            }
        });

        // Botón flecha muestra todo
        btnDrop.addActionListener(e -> {
            textField.requestFocus();
            mostrarPopup.accept(""); 
        });

        // Autoselección al hacer clic
        textField.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                SwingUtilities.invokeLater(() -> textField.selectAll());
            }
        });
        
        // Si aprieta Enter en Ventas, agarra el primer resultado válido
        if (isVentas) {
            textField.addActionListener(e -> {
                String text = textField.getText().trim().toLowerCase();
                if (text.isEmpty()) return;
                for (String op : proveedor.get()) {
                    if (op.toLowerCase().contains(text)) {
                        procesarSeleccionVenta(op);
                        textField.setText("");
                        popup.setVisible(false);
                        return;
                    }
                }
            });
        }
        
        return panel;
    }

    // Adaptador de la lista de ventas
    private List<String> obtenerListaProductosFormateada() {
        List<String> lista = new ArrayList<>();
        for (Producto p : cacheProductos) {
            lista.add(p.getCodigoBarras() + " - " + p.getTipo() + " " + p.getMarca() + " " + p.getNombre());
        }
        return lista;
    }

    private void procesarSeleccionVenta(String seleccion) {
        if(seleccion.contains(" - ")) {
            String codigo = seleccion.split(" - ")[0];
            Producto p = ProductoDAO.buscarPorCodigo(codigo);
            if (p != null && p.getStock() > 0) agregarAlCarrito(p);
            else JOptionPane.showMessageDialog(this, "Sin stock disponible");
        }
    }

    // ==============================================================
    // PESTAÑA REGISTRO
    // ==============================================================
    private void crearPestañaRegistro() {
        JPanel panelRegistro = new JPanel(new BorderLayout(10, 10));
        panelRegistro.setBorder(new EmptyBorder(15, 80, 15, 80)); 

        JPanel form = new JPanel(new GridLayout(7, 2, 10, 15));
        form.setBorder(BorderFactory.createTitledBorder("Datos del Producto"));

        Font fuenteFormulario = new Font("SansSerif", Font.PLAIN, 15);

        txtCodigoBarras = new JTextField(); txtCodigoBarras.setFont(fuenteFormulario);
        txtNombre = new JTextField(); txtNombre.setFont(fuenteFormulario);
        
        // IMPLEMENTAMOS LOS NUEVOS BUSCADORES INTELIGENTES
        txtTipo = new JTextField(); txtTipo.setFont(fuenteFormulario);
        JPanel panelTipo = crearBuscadorConDesplegable(txtTipo, () -> cacheTipos, false);
        
        txtMarca = new JTextField(); txtMarca.setFont(fuenteFormulario);
        JPanel panelMarca = crearBuscadorConDesplegable(txtMarca, () -> cacheMarcas, false);
        
        txtCosto = new JTextField(); txtCosto.setFont(fuenteFormulario);
        txtStock = new JTextField(); txtStock.setFont(fuenteFormulario);
        comboMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});
        comboMargen.setFont(fuenteFormulario);

        form.add(new JLabel("Código de Barras:") {{ setFont(fuenteFormulario); }}); form.add(txtCodigoBarras);
        form.add(new JLabel("Nombre:") {{ setFont(fuenteFormulario); }}); form.add(txtNombre);
        form.add(new JLabel("Tipo (Ej: Cargador, Cable):") {{ setFont(fuenteFormulario); }}); form.add(panelTipo);
        form.add(new JLabel("Marca (Ej: Samsung, Generico):") {{ setFont(fuenteFormulario); }}); form.add(panelMarca);
        form.add(new JLabel("Precio de Costo ($):") {{ setFont(fuenteFormulario); }}); form.add(txtCosto);
        form.add(new JLabel("Cantidad en Stock:") {{ setFont(fuenteFormulario); }}); form.add(txtStock);
        form.add(new JLabel("Margen Ganancia (%):") {{ setFont(fuenteFormulario); }}); form.add(comboMargen);

        JButton btnGuardar = new JButton("GUARDAR PRODUCTO");
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> ejecutarGuardado());

        // Navegación con Enter
        txtCodigoBarras.addActionListener(e -> txtNombre.requestFocus());
        txtNombre.addActionListener(e -> txtTipo.requestFocus());
        txtTipo.addActionListener(e -> txtMarca.requestFocus());
        txtMarca.addActionListener(e -> txtCosto.requestFocus());
        txtCosto.addActionListener(e -> txtStock.requestFocus());
        txtStock.addActionListener(e -> comboMargen.requestFocus());
        
        comboMargen.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) btnGuardar.doClick(); 
            }
        });

        panelRegistro.add(form, BorderLayout.CENTER);
        panelRegistro.add(btnGuardar, BorderLayout.SOUTH);
        pestañas.addTab(" Registrar Nuevo", panelRegistro);
    }

    // ==============================================================
    // PESTAÑA INVENTARIO Y EDICIÓN
    // ==============================================================
    private void aplicarEstiloTabla(JTable t) {
        t.setRowHeight(30); 
        t.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13)); 
    }

    private void crearPestañaInventario() {
        JPanel panelInventario = new JPanel(new BorderLayout(10, 10));
        panelInventario.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panelBusqueda = new JPanel(new BorderLayout(5, 5));
        panelBusqueda.add(new JLabel("🔍 Buscar producto: "), BorderLayout.WEST);
        JTextField txtBuscador = new JTextField();
        panelBusqueda.add(txtBuscador, BorderLayout.CENTER);
        
        JButton btnActualizarInv = new JButton("↻ Actualizar");
        btnActualizarInv.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActualizarInv.addActionListener(e -> {
            txtBuscador.setText(""); 
            if (sorterInventario != null) sorterInventario.setRowFilter(null); 
            actualizarTabla(); 
        });
        panelBusqueda.add(btnActualizarInv, BorderLayout.EAST);
        
        panelInventario.add(panelBusqueda, BorderLayout.NORTH);

        String[] columnas = {"CÓDIGO", "NOMBRE", "TIPO", "MARCA", "COSTO", "VENTA", "STOCK"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
            @Override
            public Class<?> getColumnClass(int columnIndex) { return String.class; }
        };
        tabla = new JTable(modeloTabla);
        aplicarEstiloTabla(tabla); 
        
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                try {
                    int stock = Integer.parseInt(table.getValueAt(row, 6).toString()); 
                    if (stock <= 5 && !isSelected) {
                        c.setBackground(new Color(255, 204, 204)); 
                        c.setForeground(new Color(153, 0, 0)); 
                    } else if (!isSelected) {
                        c.setBackground(table.getBackground());
                        c.setForeground(table.getForeground());
                    }
                } catch (Exception e) {}
                return c;
            }
        });

        sorterInventario = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorterInventario);

        actualizarTabla();

        txtBuscador.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) {
                String texto = txtBuscador.getText();
                if (texto.trim().length() == 0) sorterInventario.setRowFilter(null); 
                else sorterInventario.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
            }
        });

        if ("ADMIN".equals(rolActual)) {
            JPopupMenu popupMenu = new JPopupMenu();
            JMenuItem itemEditar = new JMenuItem("Editar Producto");
            JMenuItem itemEliminar = new JMenuItem("Eliminar Producto(s)");
            popupMenu.add(itemEditar);
            popupMenu.add(itemEliminar);
            tabla.setComponentPopupMenu(popupMenu);

            itemEliminar.addActionListener(e -> {
                int[] filasVisuales = tabla.getSelectedRows(); 
                if (filasVisuales.length > 0) {
                    if (JOptionPane.showConfirmDialog(this, "¿Seguro de borrar?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                        for (int i = 0; i < filasVisuales.length; i++) {
                            String codigo = modeloTabla.getValueAt(tabla.convertRowIndexToModel(filasVisuales[i]), 0).toString();
                            ProductoDAO.eliminarProducto(codigo);
                        }
                        recargarCaches(); 
                        actualizarTabla(); 
                        JOptionPane.showMessageDialog(this, "Productos eliminados.");
                    }
                }
            });

            itemEditar.addActionListener(e -> abrirEdicionDesdeTabla());
            tabla.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) abrirEdicionDesdeTabla();
                }
            });
        }
        panelInventario.add(new JScrollPane(tabla), BorderLayout.CENTER);
        pestañas.addTab(" Ver Inventario", panelInventario);
    }

    private void abrirEdicionDesdeTabla() {
        int filaVisual = tabla.getSelectedRow();
        if (filaVisual != -1) abrirDialogoEdicion(tabla.convertRowIndexToModel(filaVisual));
    }

    private void abrirDialogoEdicion(int fila) {
        String codigo = tabla.getValueAt(fila, 0).toString();
        String nAct = tabla.getValueAt(fila, 1).toString(), tAct = tabla.getValueAt(fila, 2).toString(), mAct = tabla.getValueAt(fila, 3).toString();
        String cAct = tabla.getValueAt(fila, 4).toString(), sAct = tabla.getValueAt(fila, 6).toString(); 

        JDialog dialogo = new JDialog(this, "Editar Producto", true);
        dialogo.setLayout(new GridLayout(7, 2, 10, 10)); dialogo.setSize(400, 350); dialogo.setLocationRelativeTo(this);

        JTextField txtNom = new JTextField(nAct);
        
        JTextField txtTip = new JTextField(tAct.equals("-") ? "" : tAct); 
        JPanel panelTip = crearBuscadorConDesplegable(txtTip, () -> cacheTipos, false);

        JTextField txtMar = new JTextField(mAct.equals("-") ? "" : mAct); 
        JPanel panelMar = crearBuscadorConDesplegable(txtMar, () -> cacheMarcas, false);

        JTextField txtCos = new JTextField(cAct); JTextField txtStk = new JTextField(sAct);
        JComboBox<String> cbMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});

        dialogo.add(new JLabel(" Nombre:")); dialogo.add(txtNom); 
        dialogo.add(new JLabel(" Tipo:")); dialogo.add(panelTip);
        dialogo.add(new JLabel(" Marca:")); dialogo.add(panelMar); 
        dialogo.add(new JLabel(" Costo:")); dialogo.add(txtCos);
        dialogo.add(new JLabel(" Stock:")); dialogo.add(txtStk); 
        dialogo.add(new JLabel(" Nuevo Margen %:")); dialogo.add(cbMargen);

        JButton btnConfirmar = new JButton("Guardar Cambios");
        btnConfirmar.addActionListener(ev -> {
            try {
                String nt = txtTip.getText().trim();
                String nm = txtMar.getText().trim();
                Producto pEditado = new Producto(codigo, txtNom.getText(), nt.isEmpty() ? "-" : nt, nm.isEmpty() ? "-" : nm, 
                    Double.parseDouble(txtCos.getText()), Integer.parseInt(txtStk.getText()), Double.parseDouble(cbMargen.getSelectedItem().toString()));
                
                ProductoDAO.actualizarProducto(pEditado);
                recargarCaches(); actualizarTabla(); 
                dialogo.dispose(); JOptionPane.showMessageDialog(this, "¡Producto actualizado!");
            } catch (Exception ex) { JOptionPane.showMessageDialog(dialogo, "Datos inválidos"); }
        });
        dialogo.add(new JLabel("")); dialogo.add(btnConfirmar); dialogo.setVisible(true);
    }

    // ==============================================================
    // PESTAÑA VENTAS Y CAJA
    // ==============================================================
    private void crearPestañaVentas() {
        JPanel panelVentas = new JPanel(new BorderLayout(10, 10));
        panelVentas.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel panelArriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        
        JTextField txtEscaneo = new JTextField(15);
        txtEscaneo.setBorder(BorderFactory.createTitledBorder("Escanear Código"));
        txtEscaneo.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtEscaneo.selectAll()); }
        });
        
        // BUSCADOR INTELIGENTE EN CAJA
        txtBusquedaManual = new JTextField(25);
        JPanel panelBusqueda = crearBuscadorConDesplegable(txtBusquedaManual, () -> obtenerListaProductosFormateada(), true);
        panelBusqueda.setBorder(BorderFactory.createTitledBorder("Buscar Manualmente (Tipea o abre flecha)"));

        panelArriba.add(txtEscaneo);
        panelArriba.add(panelBusqueda);
        
        String[] cols = {"CÓDIGO", "PRODUCTO", "PRECIO", "CANT.", "SUBTOTAL"};
        modeloVenta = new DefaultTableModel(cols, 0) {
            public boolean isCellEditable(int row, int column) { return false; }
        };
        tablaVenta = new JTable(modeloVenta);
        aplicarEstiloTabla(tablaVenta); 

        JPanel panelControlesCarrito = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton btnSumar = new JButton("+ Sumar 1");
        JButton btnRestar = new JButton("- Restar 1");
        JButton btnQuitar = new JButton("Quitar Todo (X)");
        btnSumar.addActionListener(e -> ajustarCantidadCarrito(1));
        btnRestar.addActionListener(e -> ajustarCantidadCarrito(-1));
        btnQuitar.addActionListener(e -> ajustarCantidadCarrito(0));
        panelControlesCarrito.add(new JLabel("Seleccione en la tabla y ajuste: "));
        panelControlesCarrito.add(btnSumar); panelControlesCarrito.add(btnRestar); panelControlesCarrito.add(btnQuitar);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(new JScrollPane(tablaVenta), BorderLayout.CENTER);
        panelCentro.add(panelControlesCarrito, BorderLayout.SOUTH);

        JPanel panelAbajo = new JPanel(new GridLayout(2, 3, 15, 10)); 
        panelAbajo.setBorder(BorderFactory.createTitledBorder("Detalles de Pago y Cobro"));

        cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito"});
        txtPagaCon = new JTextField();
        txtPagaCon.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { SwingUtilities.invokeLater(() -> txtPagaCon.selectAll()); }
        });

        lblVuelto = new JLabel("Vuelto: $0.00");
        lblVuelto.setFont(new Font("SansSerif", Font.BOLD, 18)); 

        panelAbajo.add(new JLabel("Método de Pago:")); panelAbajo.add(new JLabel("El cliente abona con ($):")); panelAbajo.add(new JLabel("")); 
        panelAbajo.add(cbMetodoPago); panelAbajo.add(txtPagaCon); panelAbajo.add(lblVuelto);
        
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelAbajo, BorderLayout.CENTER);
        
        JPanel panelCobrar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        lblTotal = new JLabel("TOTAL: $0.00"); lblTotal.setFont(new Font("SansSerif", Font.BOLD, 22));
        JButton btnFinalizar = new JButton("CONFIRMAR VENTA"); btnFinalizar.setFont(new Font("SansSerif", Font.BOLD, 14));
        panelCobrar.add(lblTotal); panelCobrar.add(btnFinalizar);
        panelSur.add(panelCobrar, BorderLayout.SOUTH);

        txtEscaneo.addActionListener(e -> {
            Producto p = ProductoDAO.buscarPorCodigo(txtEscaneo.getText());
            if (p != null && p.getStock() > 0) { agregarAlCarrito(p); txtEscaneo.setText(""); } 
            else JOptionPane.showMessageDialog(this, "Producto no encontrado o sin stock");
        });

        txtPagaCon.addKeyListener(new KeyAdapter() { public void keyReleased(KeyEvent e) { calcularVuelto(); } });
        btnFinalizar.addActionListener(e -> { if(totalVenta > 0) finalizarVenta(); });

        panelVentas.add(panelArriba, BorderLayout.NORTH);
        panelVentas.add(panelCentro, BorderLayout.CENTER); 
        panelVentas.add(panelSur, BorderLayout.SOUTH);
        pestañas.addTab("🛒 Caja y Ventas", panelVentas);
    }

    private void agregarAlCarrito(Producto p) {
        boolean productoYaExiste = false;
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            if (modeloVenta.getValueAt(i, 0).equals(p.getCodigoBarras())) {
                int cantAct = (int) modeloVenta.getValueAt(i, 3);
                if (cantAct >= p.getStock()) { JOptionPane.showMessageDialog(this, "No hay más stock disponible"); return; }
                modeloVenta.setValueAt(cantAct + 1, i, 3); 
                modeloVenta.setValueAt((cantAct + 1) * p.getPrecioVenta(), i, 4); 
                productoYaExiste = true; break;
            }
        }
        if (!productoYaExiste) modeloVenta.addRow(new Object[]{p.getCodigoBarras(), p.getNombre(), p.getPrecioVenta(), 1, p.getPrecioVenta()});
        totalVenta += p.getPrecioVenta();
        lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta));
        calcularVuelto(); 
    }

    private void finalizarVenta() {
        StringBuilder resumen = new StringBuilder();
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            String cod = modeloVenta.getValueAt(i, 0).toString();
            int cant = (int) modeloVenta.getValueAt(i, 3);
            ProductoDAO.reducirStock(cod, cant);
            resumen.append(cant).append("x ").append(modeloVenta.getValueAt(i, 1)).append(" | ");
        }
        com.tienda.db.VentaDAO.registrarVenta(resumen.toString(), totalVenta, cbMetodoPago.getSelectedItem().toString());
        JOptionPane.showMessageDialog(this, "Venta realizada con éxito");
        
        modeloVenta.setRowCount(0); totalVenta = 0;
        lblTotal.setText("TOTAL: $0.00"); txtPagaCon.setText(""); lblVuelto.setText("Vuelto: $0.00");
        
        recargarCaches(); 
        actualizarTabla(); 
        if ("ADMIN".equals(rolActual)) actualizarHistorial(); 
    }

    private void ejecutarGuardado() {
        try {
            String codigo = txtCodigoBarras.getText().trim();
            String nombre = txtNombre.getText().trim();

            if (codigo.isEmpty() || nombre.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Código y nombre obligatorios.", "Aviso", JOptionPane.WARNING_MESSAGE); return;
            }
            if (ProductoDAO.buscarPorCodigo(codigo) != null) {
                JOptionPane.showMessageDialog(this, "Ya existe código: " + codigo, "Error", JOptionPane.ERROR_MESSAGE); return;
            }
            
            String t = txtTipo.getText().trim();
            String m = txtMarca.getText().trim();
            
            Producto p = new Producto(codigo, nombre, t.isEmpty() ? "-" : t, m.isEmpty() ? "-" : m, 
                Double.parseDouble(txtCosto.getText()), Integer.parseInt(txtStock.getText()), Double.parseDouble(comboMargen.getSelectedItem().toString()));
            
            ProductoDAO.registrarProducto(p);
            JOptionPane.showMessageDialog(this, "Producto guardado");
            
            recargarCaches(); 
            actualizarTabla(); 
            
            txtCodigoBarras.setText(""); txtNombre.setText(""); txtCosto.setText(""); txtStock.setText("");
            txtTipo.setText(""); txtMarca.setText("");
            txtCodigoBarras.requestFocus();
            
        } catch (Exception ex) { JOptionPane.showMessageDialog(this, "Datos numéricos incorrectos."); }
    }

    public void actualizarTabla() {
        modeloTabla.setRowCount(0);
        for (Producto p : cacheProductos) { 
            modeloTabla.addRow(new Object[]{ p.getCodigoBarras(), p.getNombre(), p.getTipo(), p.getMarca(), p.getPrecioCosto(), p.getPrecioVenta(), p.getStock()});
        }
    }

    // ==============================================================
    // PESTAÑA HISTORIAL
    // ==============================================================
    private void crearPestañaHistorial() {
        JPanel panelHistorial = new JPanel(new BorderLayout(10, 10));
        panelHistorial.setBorder(new EmptyBorder(10, 10, 10, 10));

        JPanel panelArribaHistorial = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnActualizarHistorial = new JButton("↻ Actualizar Historial");
        btnActualizarHistorial.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActualizarHistorial.addActionListener(e -> actualizarHistorial());
        panelArribaHistorial.add(btnActualizarHistorial);
        panelHistorial.add(panelArribaHistorial, BorderLayout.NORTH);

        String[] cols = {"Nº TICKET", "FECHA Y HORA", "DETALLE DE PRODUCTOS", "TOTAL", "MÉTODO PAGO"};
        modeloHistorial = new DefaultTableModel(cols, 0) { public boolean isCellEditable(int r, int c) { return false; } };
        JTable tablaHistorial = new JTable(modeloHistorial);
        aplicarEstiloTabla(tablaHistorial); tablaHistorial.getColumnModel().getColumn(2).setPreferredWidth(300);

        actualizarHistorial();
        panelHistorial.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
        pestañas.addTab("📋 Historial de Ventas", panelHistorial);
    }

    private void actualizarHistorial() {
        if (modeloHistorial != null) {
            modeloHistorial.setRowCount(0);
            for (String[] fila : com.tienda.db.VentaDAO.obtenerHistorial()) modeloHistorial.addRow(fila);
        }
    }

    private void ajustarCantidadCarrito(int cambio) {
        int[] filas = tablaVenta.getSelectedRows(); 
        if (filas.length == 0) { JOptionPane.showMessageDialog(this, "Seleccione un producto."); return; }
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

    // CÁLCULO DE VUELTO 
   
    private void calcularVuelto() {
        try {
            if (txtPagaCon.getText().trim().isEmpty()) { 
                lblVuelto.setText("Vuelto: $0.00"); 
                lblVuelto.setForeground(UIManager.getColor("Label.foreground")); 
                return; 
            }
            double abona = Double.parseDouble(txtPagaCon.getText());
            double vuelto = abona - totalVenta;
            
            if (vuelto < 0) { 
                lblVuelto.setText("FALTAN: $" + String.format("%.2f", Math.abs(vuelto))); 
                lblVuelto.setForeground(new Color(231, 76, 60)); 
            } else { 
                lblVuelto.setText("Vuelto: $" + String.format("%.2f", vuelto)); 
                lblVuelto.setForeground(new Color(46, 204, 113)); 
            }
        } catch (NumberFormatException ex) { 
            lblVuelto.setText("Vuelto: $0.00"); 
            lblVuelto.setForeground(UIManager.getColor("Label.foreground")); 
        }
    }
}