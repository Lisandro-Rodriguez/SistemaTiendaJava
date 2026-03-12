package com.tienda.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.tienda.db.ClienteDAO;

public class PanelClientes extends JPanel {

    private DefaultTableModel modeloClientes;
    private JTable tablaClientes;
    private JComboBox<String> cbClienteVenta;
    private final String nombreUsuario;
    private Timer autoRefreshTimer;

    public PanelClientes(String nombreUsuario, JComboBox<String> cbClienteVenta) {
        this.nombreUsuario = nombreUsuario;
        this.cbClienteVenta = cbClienteVenta;
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        construir();
        iniciarAutoRefresco();
    }

    /** Refresca automáticamente las deudas cada 5 segundos */
    private void iniciarAutoRefresco() {
        autoRefreshTimer = new Timer(5000, e -> refrescar());
        autoRefreshTimer.setRepeats(true);
        autoRefreshTimer.start();
    }

    private void construir() {
        // Panel superior - formulario registro + botón actualizar
        JPanel panelArriba = new JPanel(new BorderLayout(10, 0));

        JPanel formRegistro = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        formRegistro.setBorder(BorderFactory.createTitledBorder("Registrar Nuevo Cliente en la Libreta"));

        JTextField txtNombre = new JTextField(15);
        JTextField txtTelefono = new JTextField(12);
        JButton btnAgregar = new JButton("➕ Agregar Cliente");
        btnAgregar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        formRegistro.add(new JLabel("Nombre Completo:")); formRegistro.add(txtNombre);
        formRegistro.add(new JLabel("Teléfono:")); formRegistro.add(txtTelefono);
        formRegistro.add(btnAgregar);

        // Botón actualizar en esquina superior derecha
        JButton btnActualizar = new JButton("↻ Actualizar");
        btnActualizar.setFont(new Font("SansSerif", Font.BOLD, 12));
        btnActualizar.setFocusPainted(false);
        btnActualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnActualizar.setBackground(new Color(30, 90, 160));
        btnActualizar.setForeground(Color.WHITE);
        btnActualizar.setBorderPainted(false);
        btnActualizar.addActionListener(e -> refrescar());

        JPanel panelBotonActualizar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 14));
        panelBotonActualizar.add(btnActualizar);

        panelArriba.add(formRegistro, BorderLayout.CENTER);
        panelArriba.add(panelBotonActualizar, BorderLayout.EAST);
        add(panelArriba, BorderLayout.NORTH);

        // Tabla clientes
        String[] cols = {"ID", "NOMBRE DEL CLIENTE", "TELÉFONO", "DEUDA TOTAL"};
        modeloClientes = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaClientes = new JTable(modeloClientes);
        tablaClientes.setRowHeight(30);
        tablaClientes.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tablaClientes.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tablaClientes.getTableHeader().setBackground(new Color(45, 55, 72));
        tablaClientes.getTableHeader().setForeground(Color.WHITE);
        tablaClientes.setSelectionBackground(new Color(66, 153, 225));

        // Anchos de columnas
        tablaClientes.getColumnModel().getColumn(0).setPreferredWidth(40);
        tablaClientes.getColumnModel().getColumn(1).setPreferredWidth(220);
        tablaClientes.getColumnModel().getColumn(2).setPreferredWidth(130);
        tablaClientes.getColumnModel().getColumn(3).setPreferredWidth(110);

        // Renderer: deuda en rojo si > 0, filas alternas
        tablaClientes.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                    if (column == 3) {
                        try {
                            double deuda = Double.parseDouble(value.toString().replace("$", "").replace(",", "."));
                            if (deuda > 0) {
                                c.setForeground(new Color(220, 53, 69));
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            } else {
                                c.setForeground(new Color(39, 174, 96));
                                c.setFont(c.getFont().deriveFont(Font.BOLD));
                            }
                        } catch (Exception e) { c.setForeground(table.getForeground()); }
                    } else {
                        c.setForeground(table.getForeground());
                    }
                }
                return c;
            }
        });

        // Menu contextual
        JPopupMenu menu = new JPopupMenu();
        JMenuItem itemEditar = new JMenuItem("✏️ Editar Cliente");
        JMenuItem itemEliminar = new JMenuItem("🗑️ Eliminar Cliente");
        menu.add(itemEditar); menu.add(itemEliminar);
        tablaClientes.setComponentPopupMenu(menu);

        itemEditar.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if (row != -1) {
                int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
                JTextField txtN = new JTextField(modeloClientes.getValueAt(row, 1).toString());
                JTextField txtT = new JTextField(modeloClientes.getValueAt(row, 2).toString());
                int opt = JOptionPane.showConfirmDialog(this,
                        new Object[]{"Nombre:", txtN, "Teléfono:", txtT},
                        "Editar Cliente", JOptionPane.OK_CANCEL_OPTION);
                if (opt == JOptionPane.OK_OPTION) {
                    if (ClienteDAO.actualizarCliente(id, txtN.getText().trim(), txtT.getText().trim())) {
                        refrescar();
                        JOptionPane.showMessageDialog(this, "✅ Cliente actualizado.");
                    } else {
                        JOptionPane.showMessageDialog(this, "❌ El teléfono ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        itemEliminar.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if (row != -1) {
                double deuda = Double.parseDouble(modeloClientes.getValueAt(row, 3).toString().replace("$", "").replace(",", "."));
                if (deuda > 0) {
                    JOptionPane.showMessageDialog(this, "❌ No podés eliminar un cliente con deuda pendiente.", "Acción Denegada", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
                if (JOptionPane.showConfirmDialog(this, "¿Eliminar este cliente y todo su historial?\nEsta acción no se puede deshacer.",
                        "Confirmar Eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION) {
                    ClienteDAO.eliminarCliente(id);
                    refrescar();
                    JOptionPane.showMessageDialog(this, "✅ Cliente eliminado.");
                }
            }
        });

        add(new JScrollPane(tablaClientes), BorderLayout.CENTER);

        // Panel inferior - botones acción
        JPanel panelAbajo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnVerMovimientos = new JButton("📄 Ver Movimientos");
        JButton btnAbonar = new JButton("💵 Registrar Abono");
        btnVerMovimientos.setFocusPainted(false);
        btnVerMovimientos.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAbonar.setFocusPainted(false);
        btnAbonar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAbonar.setBackground(new Color(39, 174, 96));
        btnAbonar.setForeground(Color.WHITE);
        btnAbonar.setBorderPainted(false);
        panelAbajo.add(btnVerMovimientos); panelAbajo.add(btnAbonar);
        add(panelAbajo, BorderLayout.SOUTH);

        // Acciones
        btnAgregar.addActionListener(e -> {
            String nom = txtNombre.getText().trim();
            if (nom.isEmpty()) { JOptionPane.showMessageDialog(this, "⚠️ El nombre es obligatorio."); return; }
            if (ClienteDAO.registrarCliente(nom, txtTelefono.getText().trim())) {
                txtNombre.setText(""); txtTelefono.setText("");
                refrescar();
                JOptionPane.showMessageDialog(this, "✅ Cliente registrado correctamente.");
            } else {
                JOptionPane.showMessageDialog(this, "❌ El teléfono ya está registrado.", "Teléfono Duplicado", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnAbonar.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "⚠️ Seleccioná un cliente primero."); return; }
            int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
            String nom = modeloClientes.getValueAt(row, 1).toString();
            double deudaActual = Double.parseDouble(modeloClientes.getValueAt(row, 3).toString().replace("$", "").replace(",", "."));
            String abonoStr = JOptionPane.showInputDialog(this,
                    "Cliente: " + nom + "\nDeuda actual: $" + String.format("%.2f", deudaActual) + "\n\n¿Monto del abono?",
                    "Registrar Abono", JOptionPane.QUESTION_MESSAGE);
            if (abonoStr != null && !abonoStr.trim().isEmpty()) {
                try {
                    double monto = Double.parseDouble(abonoStr.replace(",", "."));
                    if (monto <= 0) throw new NumberFormatException();
                    ClienteDAO.registrarMovimiento(id, "PAGO", monto, "Abono de deuda (" + nombreUsuario + ")");
                    refrescar();
                    JOptionPane.showMessageDialog(this, "✅ Abono de $" + String.format("%.2f", monto) + " registrado.");
                } catch (Exception ex) { JOptionPane.showMessageDialog(this, "⚠️ Monto inválido."); }
            }
        });

        btnVerMovimientos.addActionListener(e -> {
            int row = tablaClientes.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "⚠️ Seleccioná un cliente primero."); return; }
            int id = Integer.parseInt(modeloClientes.getValueAt(row, 0).toString());
            String nom = modeloClientes.getValueAt(row, 1).toString();
            mostrarHistorialCliente(id, nom);
        });

        refrescar();
    }

    private void mostrarHistorialCliente(int id, String nombre) {
        JDialog dialogo = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "📒 Libreta de " + nombre, true);
        dialogo.setSize(700, 480);
        dialogo.setLocationRelativeTo(this);

        String[] cols = {"FECHA", "TIPO", "MONTO", "DETALLE"};
        DefaultTableModel modelo = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(30);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(new Color(45, 55, 72));
        tabla.getTableHeader().setForeground(Color.WHITE);
        tabla.getColumnModel().getColumn(0).setPreferredWidth(140);
        tabla.getColumnModel().getColumn(3).setPreferredWidth(280);

        for (String[] mov : ClienteDAO.obtenerHistorialCliente(id)) modelo.addRow(mov);

        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value,
                    boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                    String tipo = table.getValueAt(row, 1).toString();
                    if (tipo.equals("PAGO")) c.setForeground(new Color(39, 174, 96));
                    else if (tipo.equals("FIADO")) c.setForeground(new Color(220, 53, 69));
                    else c.setForeground(table.getForeground());
                }
                return c;
            }
        });

        dialogo.add(new JScrollPane(tabla));
        dialogo.setVisible(true);
    }

    public void refrescar() {
        // Guardar fila seleccionada para restaurarla después del refresh
        int filaSeleccionada = tablaClientes.getSelectedRow();
        int idSeleccionado = -1;
        if (filaSeleccionada != -1)
            idSeleccionado = Integer.parseInt(modeloClientes.getValueAt(filaSeleccionada, 0).toString());

        modeloClientes.setRowCount(0);
        for (String[] c : ClienteDAO.obtenerClientes()) {
            double deuda = Double.parseDouble(c[3].replace(",", "."));
            modeloClientes.addRow(new Object[]{c[0], c[1], c[2], "$" + String.format("%.2f", deuda)});
        }

        // Restaurar selección
        if (idSeleccionado != -1) {
            for (int i = 0; i < modeloClientes.getRowCount(); i++) {
                if (Integer.parseInt(modeloClientes.getValueAt(i, 0).toString()) == idSeleccionado) {
                    tablaClientes.setRowSelectionInterval(i, i);
                    break;
                }
            }
        }

        // Actualizar combo de ventas
        if (cbClienteVenta != null) {
            String selActual = cbClienteVenta.getSelectedItem() != null ? cbClienteVenta.getSelectedItem().toString() : "0 - Consumidor Final";
            cbClienteVenta.removeAllItems();
            cbClienteVenta.addItem("0 - Consumidor Final");
            for (String[] c : ClienteDAO.obtenerClientes())
                cbClienteVenta.addItem(c[0] + " - " + c[1]);
            cbClienteVenta.setSelectedItem(selActual);
        }
    }
}
