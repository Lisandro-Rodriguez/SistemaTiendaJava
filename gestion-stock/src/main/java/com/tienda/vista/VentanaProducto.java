package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import com.tienda.Modelo.Producto;
import com.tienda.db.ProductoDAO;
import java.util.List;

public class VentanaProducto extends JFrame {

    private JTabbedPane pestañas;
    
    // Elementos de la Pestaña Registro
    private JTextField txtCodigoBarras, txtNombre, txtCosto, txtStock;
    private JComboBox<String> comboMargen;
    
    // Elementos de la Pestaña Inventario
    private JTable tabla;
    private DefaultTableModel modeloTabla;

    public VentanaProducto() {
        setTitle("Sistema de Gestión de Stock");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 1. Creamos el contenedor de pestañas
        pestañas = new JTabbedPane();

        // 2. Creamos las dos pestañas
        crearPestañaRegistro();
        crearPestañaInventario();

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

        // Agregamos el panel al TabbedPane con un nombre y un icono (opcional)
        pestañas.addTab(" Registrar Nuevo", panelRegistro);
    }

    private void crearPestañaInventario() {
        JPanel panelInventario = new JPanel(new BorderLayout(10, 10));
        panelInventario.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[] columnas = {"CÓDIGO", "NOMBRE", "COSTO", "VENTA", "STOCK"};
        modeloTabla = new DefaultTableModel(columnas, 0);
        tabla = new JTable(modeloTabla);

        actualizarTabla();

        panelInventario.add(new JScrollPane(tabla), BorderLayout.CENTER);

        JButton btnRefrescar = new JButton("Actualizar Inventario");
        btnRefrescar.addActionListener(e -> actualizarTabla());
        panelInventario.add(btnRefrescar, BorderLayout.SOUTH);

        pestañas.addTab(" Ver Inventario", panelInventario);
    }

    private void ejecutarGuardado() {
        try {
            double costo = Double.parseDouble(txtCosto.getText());
            double margen = Double.parseDouble(comboMargen.getSelectedItem().toString());
            
            Producto p = new Producto(txtCodigoBarras.getText(), txtNombre.getText(), costo, Integer.parseInt(txtStock.getText()), margen);
            ProductoDAO.registrarProducto(p);
            
            JOptionPane.showMessageDialog(this, "Producto guardado exitosamente");
            
            actualizarTabla(); // Actualiza la tabla de la otra pestaña
            limpiarCampos();
            
            // Opcional: Saltar automáticamente a la pestaña de inventario al guardar
            // pestañas.setSelectedIndex(1); 

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
}