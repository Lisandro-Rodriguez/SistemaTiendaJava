package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import com.tienda.Modelo.Producto; // Asegúrate de que la "m" de modelo sea minúscula según tu carpeta
import com.tienda.db.ProductoDAO;
import java.util.List;

public class VentanaProducto extends JFrame {
    
    // --- VARIABLES GLOBALES ---
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

    public VentanaProducto() {
        setTitle("Sistema de Gestión de Stock y Ventas");
        setSize(900, 650); // Lo hice un poquito más grande para que entre todo cómodo
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        pestañas = new JTabbedPane();

        crearPestañaRegistro();
        crearPestañaInventario();
        crearPestañaVentas();

        add(pestañas);
    }

    private void crearPestañaRegistro() {
        JPanel panelRegistro = new JPanel(new BorderLayout(10, 10));
        panelRegistro.setBorder(new EmptyBorder(20, 50, 20, 50));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 20));
        form.setBorder(new TitledBorder("Datos del Producto"));

        txtCodigoBarras = new JTextField();
        txtNombre = new JTextField();
        txtCosto = new JTextField();
        txtStock = new JTextField();
        comboMargen = new JComboBox<>(new String[]{"10", "20", "30", "40", "50", "100"});

        form.add(new JLabel("Código de Barras:")); form.add(txtCodigoBarras);
        form.add(new JLabel("Nombre:")); form.add(txtNombre);
        form.add(new JLabel("Precio de Costo ($):")); form.add(txtCosto);
        form.add(new JLabel("Cantidad en Stock:")); form.add(txtStock);
        form.add(new JLabel("Margen de Ganancia (%):")); form.add(comboMargen);

        JButton btnGuardar = new JButton("GUARDAR PRODUCTO");
        btnGuardar.setBackground(new Color(46, 204, 113));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14));

        btnGuardar.addActionListener(e -> ejecutarGuardado());

        panelRegistro.add(form, BorderLayout.CENTER);
        panelRegistro.add(btnGuardar, BorderLayout.SOUTH);

        pestañas.addTab(" Registrar Nuevo", panelRegistro);
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
        modeloTabla = new DefaultTableModel(columnas, 0);
        tabla = new JTable(modeloTabla);
        
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

        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem itemEditar = new JMenuItem("Editar Producto");
        JMenuItem itemEliminar = new JMenuItem("Eliminar Producto(s)");

        popupMenu.add(itemEditar);
        popupMenu.add(itemEliminar);
        tabla.setComponentPopupMenu(popupMenu);

        itemEliminar.addActionListener(e -> {
            int[] filasVisuales = tabla.getSelectedRows(); 
            if (filasVisuales.length > 0) {
                int respuesta = JOptionPane.showConfirmDialog(this, 
                    "¿Seguro que quieres borrar " + filasVisuales.length + " producto(s)?", "Confirmar", JOptionPane.YES_NO_OPTION);
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

        panelInventario.add(new JScrollPane(tabla), BorderLayout.CENTER);
        pestañas.addTab(" Ver Inventario", panelInventario);
    }
    
    private void crearPestañaVentas() {
        JPanel panelVentas = new JPanel(new BorderLayout(10, 10));
        panelVentas.setBorder(new EmptyBorder(10, 10, 10, 10));
        
        // --- ZONA ARRIBA ---
        JPanel panelArriba = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        
        JTextField txtEscaneo = new JTextField(15);
        txtEscaneo.setBorder(new TitledBorder("1. Escanear Código"));
        
        JComboBox<String> cbBusquedaManual = new JComboBox<>();
        cbBusquedaManual.setBorder(new TitledBorder("2. O Buscar Manualmente"));
        cbBusquedaManual.addItem("Seleccione un producto...");
        cbBusquedaManual.setEditable(true);
        
        JTextField txtFiltroCombo = (JTextField) cbBusquedaManual.getEditor().getEditorComponent();

        // Borra el texto de ayuda al hacer click
        txtFiltroCombo.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent e) {
                if (txtFiltroCombo.getText().equals("Seleccione un producto...")) {
                    txtFiltroCombo.setText("");
                }
            }
        });

        // ==========================================
        // 1. NUEVA LÓGICA DEL BUSCADOR MANUAL (TECLADO + ENTER)
        // ==========================================
        txtFiltroCombo.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent e) {
                // A. Si presiona ENTER (Cargar producto al carrito rápido)
                if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
                    String seleccion = txtFiltroCombo.getText();
                    if(seleccion.contains(" - ")) {
                        String codigo = seleccion.split(" - ")[0];
                        Producto p = ProductoDAO.buscarPorCodigo(codigo);
                        if (p != null && p.getStock() > 0) {
                            agregarAlCarrito(p);
                            txtFiltroCombo.setText(""); // Limpia para el siguiente cliente
                            cbBusquedaManual.hidePopup(); // Cierra la lista desplegable
                        } else {
                            JOptionPane.showMessageDialog(null, "Sin stock disponible");
                        }
                    }
                    return; // Cortamos acá para que no siga filtrando
                }

                // B. Si usa las flechitas arriba/abajo (Navegación normal)
                if(e.getKeyCode() == java.awt.event.KeyEvent.VK_UP || 
                   e.getKeyCode() == java.awt.event.KeyEvent.VK_DOWN) {
                    return; 
                }

                // C. Si está escribiendo letras (Filtramos la lista)
                SwingUtilities.invokeLater(() -> {
                    String texto = txtFiltroCombo.getText();
                    cbBusquedaManual.removeAllItems();
                    if (texto.isEmpty()) cbBusquedaManual.addItem("Seleccione un producto...");
                    
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
        
        // --- ZONA CENTRO (TABLA Y BOTONES) ---
        String[] cols = {"CÓDIGO", "PRODUCTO", "PRECIO", "CANT.", "SUBTOTAL"};
        modeloVenta = new DefaultTableModel(cols, 0);
        tablaVenta = new JTable(modeloVenta);

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

        // --- ZONA ABAJO ---
        JPanel panelAbajo = new JPanel(new GridLayout(2, 3, 15, 10)); 
        panelAbajo.setBorder(new TitledBorder("Detalles de Pago y Cobro"));

        JComboBox<String> cbMetodoPago = new JComboBox<>(new String[]{"Efectivo", "Transferencia", "Tarjeta Débito", "Tarjeta Crédito"});
        txtPagaCon = new JTextField();
        lblVuelto = new JLabel("Vuelto: $0.00");
        lblVuelto.setFont(new Font("SansSerif", Font.BOLD, 16));
        lblVuelto.setForeground(new Color(192, 57, 43));

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
        btnFinalizar.setBackground(new Color(46, 204, 113));
        btnFinalizar.setForeground(Color.WHITE);
        btnFinalizar.setFont(new Font("SansSerif", Font.BOLD, 14));

        panelCobrar.add(lblTotal);
        panelCobrar.add(btnFinalizar);
        panelSur.add(panelCobrar, BorderLayout.SOUTH);

        // --- EVENTOS ---
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

        // ==========================================
        // 2. NUEVA LÓGICA DEL BUSCADOR MANUAL (CLICK DEL MOUSE)
        // ==========================================
        cbBusquedaManual.addActionListener(e -> {
            // Solo lo activamos si el usuario hizo clic real en la lista
            if (e.getActionCommand().equals("comboBoxChanged") && !cbBusquedaManual.isPopupVisible()) {
                if (cbBusquedaManual.getSelectedItem() != null) {
                    String seleccion = cbBusquedaManual.getSelectedItem().toString();
                    if(seleccion.contains(" - ")) {
                        String codigo = seleccion.split(" - ")[0]; 
                        Producto p = ProductoDAO.buscarPorCodigo(codigo);
                        if (p != null && p.getStock() > 0) {
                            agregarAlCarrito(p);
                            txtFiltroCombo.setText(""); // Dejamos limpio
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
            txtPagaCon.setText("");
            lblVuelto.setText("Vuelto: $0.00");
            cbMetodoPago.setSelectedIndex(0);
        });

        // ARMADO FINAL DE LA PESTAÑA VENTAS 
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
        for (int i = 0; i < modeloVenta.getRowCount(); i++) {
            String cod = modeloVenta.getValueAt(i, 0).toString();
            int cant = (int) modeloVenta.getValueAt(i, 3);
            ProductoDAO.reducirStock(cod, cant);
        }
        JOptionPane.showMessageDialog(this, "Venta realizada con éxito");
        modeloVenta.setRowCount(0);
        totalVenta = 0;
        lblTotal.setText("TOTAL: $0.00");
        actualizarTabla(); 
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
        dialogo.setSize(300, 250);
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
        int[] filas = tablaVenta.getSelectedRows(); // <-- AHORA OBTIENE TODAS LAS SELECCIONADAS
        if (filas.length == 0) {
            JOptionPane.showMessageDialog(this, "Por favor, seleccione al menos un producto del carrito.");
            return;
        }

        // IMPORTANTE: Recorremos de ATRÁS hacia ADELANTE para que al borrar filas no se corran los índices
        for (int i = filas.length - 1; i >= 0; i--) {
            int fila = filas[i];
            int cantActual = (int) modeloVenta.getValueAt(fila, 3);
            double precioUnidad = (double) modeloVenta.getValueAt(fila, 2);
            String codigo = modeloVenta.getValueAt(fila, 0).toString();

            if (cambio == 0) { // Eliminar todo el producto de la fila
                totalVenta -= (cantActual * precioUnidad);
                modeloVenta.removeRow(fila);
            } else {
                int nuevaCant = cantActual + cambio;
                if (nuevaCant <= 0) { // Si llega a cero al restar, se borra de la lista
                    totalVenta -= (cantActual * precioUnidad);
                    modeloVenta.removeRow(fila);
                } else {
                    if (cambio > 0) {
                        Producto p = ProductoDAO.buscarPorCodigo(codigo);
                        if (nuevaCant > p.getStock()) {
                            JOptionPane.showMessageDialog(this, "No hay más stock para: " + p.getNombre());
                            continue; // Salta al siguiente producto seleccionado sin causar error
                        }
                    }
                    modeloVenta.setValueAt(nuevaCant, fila, 3);
                    modeloVenta.setValueAt(nuevaCant * precioUnidad, fila, 4);
                    totalVenta += (cambio * precioUnidad);
                }
            }
        }

        lblTotal.setText("TOTAL: $" + String.format("%.2f", totalVenta));
        calcularVuelto(); // Sincronizamos el vuelto
    }
}