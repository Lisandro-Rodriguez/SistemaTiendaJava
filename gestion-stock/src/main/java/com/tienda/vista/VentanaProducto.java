package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import com.tienda.Modelo.Producto; 
import com.tienda.db.ProductoDAO;
import java.util.List;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.FlatLaf;

public class VentanaProducto extends JFrame {
    
    private JTabbedPane pestañas;
    
    // Pestaña Registro
    private JTextField txtCodigoBarras, txtNombre, txtCosto, txtStock;
    private JComboBox<String> comboMargen;
    
    // Pestaña Inventario
    private JTable tabla;
    private DefaultTableModel modeloTabla;
    
    // Pestaña Ventas
    private DefaultTableModel modeloVenta;
    private JTable tablaVenta; 
    private JLabel lblTotal;
    private double totalVenta = 0;
    private JTextField txtPagaCon;
    private JLabel lblVuelto;
    
    // Pestaña Historial y Pagos
    private JComboBox<String> cbMetodoPago; 
    private DefaultTableModel modeloHistorial;
    
    // ¡NUEVO! Guardamos el rol para saber qué permisos darle en otras pestañas
    private String rolActual; 

    public VentanaProducto(String rol) {
        this.rolActual = rol; // Guardamos el rol apenas entra

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

        // Control de pestañas según el rol
        if (rol.equals("ADMIN")) {
            crearPestañaRegistro();
            crearPestañaInventario();
            crearPestañaVentas();
            crearPestañaHistorial(); 
        } else if (rol.equals("CAJERO")) {
            crearPestañaVentas();
            crearPestañaInventario(); // El cajero la ve, pero ahora bloquearemos su edición
        }

        // --- BARRA SUPERIOR (RELOJ, MODO OSCURO Y CERRAR SESIÓN) ---
        JPanel panelTop = new JPanel(new BorderLayout());
        panelTop.setBorder(new EmptyBorder(10, 15, 10, 15));

        // Botón Cerrar Sesión
        JButton btnCerrarSesion = new JButton("🚪 Cerrar Sesión");
        btnCerrarSesion.setFocusPainted(false);
        btnCerrarSesion.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnCerrarSesion.setBackground(new Color(231, 76, 60)); // Rojo suave
        btnCerrarSesion.setForeground(Color.WHITE);
        btnCerrarSesion.addActionListener(e -> {
            this.dispose(); // Cierra la ventana principal
            new VentanaLogin().setVisible(true); // Abre el Login de nuevo
        });

        JToggleButton btnTema = new JToggleButton("🌙 Modo Oscuro");
        btnTema.setFocusPainted(false);
        btnTema.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTema.addActionListener(e -> aplicarTema(btnTema.isSelected(), btnTema));

        // Agrupamos los dos botones a la izquierda
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
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error al cambiar el tema");
        }
    }

    private void crearPestañaRegistro() {
        JPanel panelRegistro = new JPanel(new BorderLayout(10, 10));
        panelRegistro.setBorder(new EmptyBorder(30, 80, 30, 80));

        JPanel form = new JPanel(new GridLayout(5, 2, 15, 25));
        form.setBorder(BorderFactory.createTitledBorder("Datos del Producto"));

        Font fuenteFormulario = new Font("SansSerif", Font.PLAIN, 15);

        txtCodigoBarras = new JTextField(); txtCodigoBarras.setFont(fuenteFormulario);
        txtNombre = new JTextField(); txtNombre.setFont(fuenteFormulario);
        txtCosto = new JTextField(); txtCosto.setFont(fuenteFormulario);
        txtStock = new JTextField(); txtStock.setFont(fuenteFormulario);
        comboMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});
        comboMargen.setFont(fuenteFormulario);

        JLabel lbl1 = new JLabel("Código de Barras:"); lbl1.setFont(fuenteFormulario);
        JLabel lbl2 = new JLabel("Nombre:"); lbl2.setFont(fuenteFormulario);
        JLabel lbl3 = new JLabel("Precio de Costo ($):"); lbl3.setFont(fuenteFormulario);
        JLabel lbl4 = new JLabel("Cantidad en Stock:"); lbl4.setFont(fuenteFormulario);
        JLabel lbl5 = new JLabel("Margen de Ganancia (%):"); lbl5.setFont(fuenteFormulario);

        form.add(lbl1); form.add(txtCodigoBarras);
        form.add(lbl2); form.add(txtNombre);
        form.add(lbl3); form.add(txtCosto);
        form.add(lbl4); form.add(txtStock);
        form.add(lbl5); form.add(comboMargen);

        JButton btnGuardar = new JButton("GUARDAR PRODUCTO");
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnGuardar.addActionListener(e -> ejecutarGuardado());

        panelRegistro.add(form, BorderLayout.CENTER);
        panelRegistro.add(btnGuardar, BorderLayout.SOUTH);

        pestañas.addTab(" Registrar Nuevo", panelRegistro);
    }

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
        
        panelInventario.add(panelBusqueda, BorderLayout.NORTH);

        String[] columnas = {"CÓDIGO", "NOMBRE", "COSTO", "VENTA", "STOCK"};
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        tabla = new JTable(modeloTabla);
        aplicarEstiloTabla(tabla); 
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modeloTabla);
        tabla.setRowSorter(sorter);

        actualizarTabla();

        txtBuscador.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String texto = txtBuscador.getText();
                if (texto.trim().length() == 0) {
                    sorter.setRowFilter(null); 
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + texto));
                }
            }
        });

        // ====================================================
        // MAGIA DE SEGURIDAD: SOLO EL ADMIN PUEDE EDITAR/BORRAR
        // ====================================================
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
                    int respuesta = JOptionPane.showConfirmDialog(this, "¿Seguro que quieres borrar " + filasVisuales.length + " producto(s)?", "Confirmar", JOptionPane.YES_NO_OPTION);
                    if (respuesta == JOptionPane.YES_OPTION) {
                        for (int i = 0; i < filasVisuales.length; i++) {
                            int filaReal = tabla.convertRowIndexToModel(filasVisuales[i]);
                            String codigo = modeloTabla.getValueAt(filaReal, 0).toString();
                            ProductoDAO.eliminarProducto(codigo);
                        }
                        actualizarTabla(); 
                        JOptionPane.showMessageDialog(this, "Productos eliminados.");
                    }
                }
            });

            itemEditar.addActionListener(e -> {
                int filaVisual = tabla.getSelectedRow();
                if (filaVisual != -1) {
                    int filaReal = tabla.convertRowIndexToModel(filaVisual);
                    abrirDialogoEdicion(filaReal);
                }
            });

            tabla.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                        int filaVisual = tabla.getSelectedRow();
                        if (filaVisual != -1) {
                            int filaReal = tabla.convertRowIndexToModel(filaVisual);
                            abrirDialogoEdicion(filaReal); 
                        }
                    }
                }
            });
        }
        // Si es CAJERO, el bloque de arriba se ignora y la tabla queda de "Solo lectura" pura.

        panelInventario.add(new JScrollPane(tabla), BorderLayout.CENTER);
        pestañas.addTab(" Ver Inventario", panelInventario);
    }
    
    private void crearPestañaVentas() {
        JPanel panelVentas = new JPanel(new BorderLayout(10, 10));
        panelVentas.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        JPanel panelArriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        
        JTextField txtEscaneo = new JTextField(15);
        txtEscaneo.setBorder(BorderFactory.createTitledBorder("Escanear Código"));
        
        JComboBox<String> cbBusquedaManual = new JComboBox<>();
        cbBusquedaManual.setBorder(BorderFactory.createTitledBorder("Buscar Manualmente"));
        cbBusquedaManual.addItem("Seleccione un producto...");
        cbBusquedaManual.setEditable(true);
        
        JTextField txtFiltroCombo = (JTextField) cbBusquedaManual.getEditor().getEditorComponent();

        txtFiltroCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txtFiltroCombo.getText().equals("Seleccione un producto...")) {
                    txtFiltroCombo.setText("");
                }
            }
        });

        txtFiltroCombo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    String seleccion = txtFiltroCombo.getText();
                    if(seleccion.contains(" - ")) {
                        String codigo = seleccion.split(" - ")[0];
                        Producto p = ProductoDAO.buscarPorCodigo(codigo);
                        if (p != null && p.getStock() > 0) {
                            agregarAlCarrito(p);
                            txtFiltroCombo.setText(""); 
                            cbBusquedaManual.hidePopup(); 
                        } else {
                            JOptionPane.showMessageDialog(null, "Sin stock disponible");
                        }
                    }
                    return; 
                }

                if(e.getKeyCode() == java.awt.event.KeyEvent.VK_UP || e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    return; 
                }

                SwingUtilities.invokeLater(() -> {
                    String texto = txtFiltroCombo.getText();
                    cbBusquedaManual.removeAllItems();
                    if (texto.isEmpty()) {
                        cbBusquedaManual.addItem("Seleccione un producto...");
                    }
                    for(Producto prod : ProductoDAO.obtenerTodos()) {
                        String item = prod.getCodigoBarras() + " - " + prod.getNombre();
                        if(item.toLowerCase().contains(texto.toLowerCase())) {
                            cbBusquedaManual.addItem(item);
                        }
                    }
                    txtFiltroCombo.setText(texto);
                    if(cbBusquedaManual.getItemCount() > 0) cbBusquedaManual.showPopup(); 
                });
            }
        });

        for(Producto p : ProductoDAO.obtenerTodos()) {
            cbBusquedaManual.addItem(p.getCodigoBarras() + " - " + p.getNombre());
        }

        panelArriba.add(txtEscaneo);
        panelArriba.add(cbBusquedaManual);
        
        String[] cols = {"CÓDIGO", "PRODUCTO", "PRECIO", "CANT.", "SUBTOTAL"};
        modeloVenta = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
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
        panelControlesCarrito.add(btnSumar);
        panelControlesCarrito.add(btnRestar);
        panelControlesCarrito.add(btnQuitar);

        JPanel panelCentro = new JPanel(new BorderLayout());
        panelCentro.add(new JScrollPane(tablaVenta), BorderLayout.CENTER);
        panelCentro.add(panelControlesCarrito, BorderLayout.SOUTH);

        JPanel panelAbajo = new JPanel(new GridLayout(2, 3, 15, 10)); 
        panelAbajo.setBorder(BorderFactory.createTitledBorder("Detalles de Pago y Cobro"));

        cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito"});
        txtPagaCon = new JTextField();
        lblVuelto = new JLabel("Vuelto: $0.00");
        lblVuelto.setFont(new Font("SansSerif", Font.BOLD, 18)); 

        panelAbajo.add(new JLabel("Método de Pago:"));
        panelAbajo.add(new JLabel("El cliente abona con ($):"));
        panelAbajo.add(new JLabel("")); 
        panelAbajo.add(cbMetodoPago);
        panelAbajo.add(txtPagaCon);
        panelAbajo.add(lblVuelto);
        
        JPanel panelSur = new JPanel(new BorderLayout());
        panelSur.add(panelAbajo, BorderLayout.CENTER);
        
        JPanel panelCobrar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 20, 10));
        lblTotal = new JLabel("TOTAL: $0.00");
        lblTotal.setFont(new Font("SansSerif", Font.BOLD, 22));
        
        JButton btnFinalizar = new JButton("CONFIRMAR VENTA");
        btnFinalizar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnFinalizar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelCobrar.add(lblTotal);
        panelCobrar.add(btnFinalizar);
        panelSur.add(panelCobrar, BorderLayout.SOUTH);

        txtEscaneo.addActionListener(e -> {
            String codigo = txtEscaneo.getText();
            Producto p = ProductoDAO.buscarPorCodigo(codigo);
            if (p != null && p.getStock() > 0) {
                agregarAlCarrito(p);
                txtEscaneo.setText(""); 
            } else {
                JOptionPane.showMessageDialog(this, "Producto no encontrado o sin stock");
            }
        });

        cbBusquedaManual.addActionListener(e -> {
            if (e.getActionCommand().equals("comboBoxChanged") && !cbBusquedaManual.isPopupVisible()) {
                if (cbBusquedaManual.getSelectedItem() != null) {
                    String seleccion = cbBusquedaManual.getSelectedItem().toString();
                    if(seleccion.contains(" - ")) {
                        String codigo = seleccion.split(" - ")[0]; 
                        Producto p = ProductoDAO.buscarPorCodigo(codigo);
                        if (p != null && p.getStock() > 0) {
                            agregarAlCarrito(p);
                            txtFiltroCombo.setText(""); 
                        } else {
                            JOptionPane.showMessageDialog(this, "Sin stock disponible");
                        }
                    }
                }
            }
        });

        txtPagaCon.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                calcularVuelto();
            }
        });

        btnFinalizar.addActionListener(e -> {
            if(totalVenta == 0) return; 
            finalizarVenta(); 
        });

        panelVentas.add(panelArriba, BorderLayout.NORTH);
        panelVentas.add(panelCentro, BorderLayout.CENTER); 
        panelVentas.add(panelSur, BorderLayout.SOUTH);

        pestañas.addTab("🛒 Caja y Ventas", panelVentas);
    }

    private void agregarAlCarrito(Producto p) {
        boolean productoYaExiste = false;
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            if (modeloVenta.getValueAt(i, 0).equals(p.getCodigoBarras())) {
                int cantidadActual = (int) modeloVenta.getValueAt(i, 3);
                if (cantidadActual >= p.getStock()) {
                    JOptionPane.showMessageDialog(this, "No hay más stock disponible para: " + p.getNombre());
                    return;
                }
                int nuevaCantidad = cantidadActual + 1;
                double nuevoSubtotal = nuevaCantidad * p.getPrecioVenta();
                modeloVenta.setValueAt(nuevaCantidad, i, 3); 
                modeloVenta.setValueAt(nuevoSubtotal, i, 4); 
                productoYaExiste = true;
                break;
            }
        }
        if (!productoYaExiste) {
            modeloVenta.addRow(new Object[]{p.getCodigoBarras(), p.getNombre(), p.getPrecioVenta(), 1, p.getPrecioVenta()});
        }
        totalVenta += p.getPrecioVenta();
        lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta));
        calcularVuelto(); 
    }

    private void finalizarVenta() {
        StringBuilder resumenProductos = new StringBuilder();

        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            String cod = modeloVenta.getValueAt(i, 0).toString();
            String nombre = modeloVenta.getValueAt(i, 1).toString();
            int cant = (int) modeloVenta.getValueAt(i, 3);
            
            ProductoDAO.reducirStock(cod, cant);
            resumenProductos.append(cant).append("x ").append(nombre).append(" | ");
        }
        
        String metodo = cbMetodoPago.getSelectedItem().toString();
        com.tienda.db.VentaDAO.registrarVenta(resumenProductos.toString(), totalVenta, metodo);

        JOptionPane.showMessageDialog(this, "Venta realizada y guardada en el historial con éxito");
        
        modeloVenta.setRowCount(0);
        totalVenta = 0;
        lblTotal.setText("TOTAL: $0.00");
        txtPagaCon.setText("");
        lblVuelto.setText("Vuelto: $0.00");
        cbMetodoPago.setSelectedIndex(0);
        
        actualizarTabla(); 
        if ("ADMIN".equals(rolActual)) {
            actualizarHistorial(); 
        }
    }

    private void ejecutarGuardado() {
        try {
            double costo = Double.parseDouble(txtCosto.getText());
            double margen = Double.parseDouble(comboMargen.getSelectedItem().toString());
            Producto p = new Producto(txtCodigoBarras.getText(), txtNombre.getText(), costo, Integer.parseInt(txtStock.getText()), margen);
            ProductoDAO.registrarProducto(p);
            JOptionPane.showMessageDialog(this, "Producto guardado exitosamente");
            actualizarTabla(); 
            limpiarCampos();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error: Verifique los datos");
        }
    }

    public void actualizarTabla() {
        modeloTabla.setRowCount(0);
        List<Producto> lista = ProductoDAO.obtenerTodos();
        for (Producto p : lista) {
            modeloTabla.addRow(new Object[]{p.getCodigoBarras(), p.getNombre(), p.getPrecioCosto(), p.getPrecioVenta(), p.getStock()});
        }
    }

    private void limpiarCampos() {
        txtCodigoBarras.setText(""); txtNombre.setText(""); txtCosto.setText(""); txtStock.setText("");
        txtCodigoBarras.requestFocus();
    }

    private void abrirDialogoEdicion(int fila) {
        String codigo = tabla.getValueAt(fila, 0).toString();
        String nombreActual = tabla.getValueAt(fila, 1).toString();
        String costoActual = tabla.getValueAt(fila, 2).toString();
        String stockActual = tabla.getValueAt(fila, 4).toString();

        JDialog dialogo = new JDialog(this, "Editar Producto", true);
        dialogo.setLayout(new GridLayout(5, 2, 10, 10));
        dialogo.setSize(350, 280);
        dialogo.setLocationRelativeTo(this);

        JTextField txtNom = new JTextField(nombreActual);
        JTextField txtCos = new JTextField(costoActual);
        JTextField txtStk = new JTextField(stockActual);
        JComboBox<String> cbMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});

        dialogo.add(new JLabel(" Nombre:")); dialogo.add(txtNom);
        dialogo.add(new JLabel(" Costo:")); dialogo.add(txtCos);
        dialogo.add(new JLabel(" Stock:")); dialogo.add(txtStk);
        dialogo.add(new JLabel(" Nuevo Margen %:")); dialogo.add(cbMargen);

        JButton btnConfirmar = new JButton("Guardar Cambios");
        btnConfirmar.addActionListener(ev -> {
            try {
                double nuevoCosto = Double.parseDouble(txtCos.getText());
                int nuevoStock = Integer.parseInt(txtStk.getText());
                double nuevoMargen = Double.parseDouble(cbMargen.getSelectedItem().toString());

                Producto pEditado = new Producto(codigo, txtNom.getText(), nuevoCosto, nuevoStock, nuevoMargen);
                ProductoDAO.actualizarProducto(pEditado);
                actualizarTabla();
                dialogo.dispose(); 
                JOptionPane.showMessageDialog(this, "¡Producto actualizado!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialogo, "Datos inválidos");
            }
        });

        dialogo.add(new JLabel("")); 
        dialogo.add(btnConfirmar);
        dialogo.setVisible(true);
    }

    private void calcularVuelto() {
        try {
            if (txtPagaCon.getText().trim().isEmpty()) {
                lblVuelto.setText("Vuelto: $0.00");
                return;
            }
            double abona = Double.parseDouble(txtPagaCon.getText());
            double vuelto = abona - totalVenta;
            
            if (vuelto < 0) vuelto = 0; 
            lblVuelto.setText("Vuelto: $" + String.format("%.2f", vuelto));
        } catch (NumberFormatException ex) {
            lblVuelto.setText("Vuelto: $0.00");
        }
    }

    private void ajustarCantidadCarrito(int cambio) {
        int[] filas = tablaVenta.getSelectedRows(); 
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione al menos un producto del carrito.");
            return;
        }

        for (int i = filas.length - 1; i >= 0; i--) {
            int fila = filas[i];
            int cantActual = (int) modeloVenta.getValueAt(fila, 3);
            double precioUnidad = (double) modeloVenta.getValueAt(fila, 2);
            String codigo = modeloVenta.getValueAt(fila, 0).toString();

            if (cambio == 0) { 
                totalVenta -= (cantActual * precioUnidad);
                modeloVenta.removeRow(fila);
            } else {
                int nuevaCant = cantActual + cambio;
                if (nuevaCant <= 0) { 
                    totalVenta -= (cantActual * precioUnidad);
                    modeloVenta.removeRow(fila);
                } else {
                    if (cambio > 0) {
                        Producto p = ProductoDAO.buscarPorCodigo(codigo);
                        if (nuevaCant > p.getStock()) {
                            JOptionPane.showMessageDialog(this, "No hay más stock para: " + p.getNombre());
                            continue; 
                        }
                    }
                    modeloVenta.setValueAt(nuevaCant, fila, 3);
                    modeloVenta.setValueAt(nuevaCant * precioUnidad, fila, 4);
                    totalVenta += (cambio * precioUnidad);
                }
            }
        }

        lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta));
        calcularVuelto(); 
    }

    private void crearPestañaHistorial() {
        JPanel panelHistorial = new JPanel(new BorderLayout(10, 10));
        panelHistorial.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columnas = {"Nº TICKET", "FECHA Y HORA", "DETALLE DE PRODUCTOS", "TOTAL", "MÉTODO PAGO"};
        modeloHistorial = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; 
            }
        };
        JTable tablaHistorial = new JTable(modeloHistorial);
        aplicarEstiloTabla(tablaHistorial); 
        
        tablaHistorial.getColumnModel().getColumn(2).setPreferredWidth(300);

        actualizarHistorial();

        JButton btnRefrescar = new JButton("Actualizar Historial");
        btnRefrescar.addActionListener(e -> actualizarHistorial());

        panelHistorial.add(new JScrollPane(tablaHistorial), BorderLayout.CENTER);
        panelHistorial.add(btnRefrescar, BorderLayout.SOUTH);

        pestañas.addTab("📋 Historial de Ventas", panelHistorial);
    }

    private void actualizarHistorial() {
        if (modeloHistorial != null) {
            modeloHistorial.setRowCount(0);
            List<String[]> historial = com.tienda.db.VentaDAO.obtenerHistorial();
            for (String[] fila : historial) {
                modeloHistorial.addRow(fila);
            }
        }
    }
}