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

        // --- CREAMOS EL MENÚ EMERGENTE (CLICK DERECHO) ---
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem itemEditar = new JMenuItem("Editar Producto");
        JMenuItem itemEliminar = new JMenuItem("Eliminar Producto");

         popupMenu.add(itemEditar);
         popupMenu.add(itemEliminar);

        // Asignamos el menú a la tabla
        tabla.setComponentPopupMenu(popupMenu);

         // --- LÓGICA PARA ELIMINAR ---
        itemEliminar.addActionListener(e -> {
        int fila = tabla.getSelectedRow();
        if (fila != -1) {
            String codigo = tabla.getValueAt(fila, 0).toString();
            String nombre = tabla.getValueAt(fila, 1).toString();

            int respuesta = JOptionPane.showConfirmDialog(this, 
                "¿Seguro que quieres borrar " + nombre + "?", "Confirmar", JOptionPane.YES_NO_OPTION);
            
            if (respuesta == JOptionPane.YES_OPTION) {
                ProductoDAO.eliminarProducto(codigo);
                actualizarTabla(); // Refrescamos la pestaña para que desaparezca
            }
        }
        });

        // --- LÓGICA PARA EDITAR ---
        itemEditar.addActionListener(e -> {
             int fila = tabla.getSelectedRow();
                if (fila != -1) {
                  abrirDialogoEdicion(fila);
            }
        });
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

    private void abrirDialogoEdicion(int fila) {
    // 1. Obtenemos los datos actuales de la fila tocada
    String codigo = tabla.getValueAt(fila, 0).toString();
    String nombreActual = tabla.getValueAt(fila, 1).toString();
    String costoActual = tabla.getValueAt(fila, 2).toString();
    String stockActual = tabla.getValueAt(fila, 4).toString();

    // 2. Creamos una ventanita rápida
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

            // Creamos el objeto con los cambios (el constructor calcula el nuevo precio_venta)
            Producto pEditado = new Producto(codigo, txtNom.getText(), nuevoCosto, nuevoStock, nuevoMargen);
            
            ProductoDAO.actualizarProducto(pEditado);
            actualizarTabla();
            dialogo.dispose(); // Cerramos la ventanita
            JOptionPane.showMessageDialog(this, "¡Producto actualizado!");
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dialogo, "Datos inválidos");
        }
    });

    dialogo.add(new JLabel("")); // Espacio
    dialogo.add(btnConfirmar);
    dialogo.setVisible(true);
}
}