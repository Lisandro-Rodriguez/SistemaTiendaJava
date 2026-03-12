package com.tienda.vista;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.tienda.db.UsuarioDAO;

public class PanelUsuarios extends JPanel {

    private final String nombreUsuario;
    private DefaultTableModel modeloUsuarios;

    public PanelUsuarios(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
        setLayout(new BorderLayout(15, 15));
        setBorder(new EmptyBorder(20, 20, 20, 20));
        construir();
    }

    private void construir() {
        JPanel panelForm = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 10));
        panelForm.setBorder(BorderFactory.createTitledBorder("Crear Nueva Cuenta de Empleado"));

        JTextField txtUsuario = new JTextField(12);
        JPasswordField txtPass = new JPasswordField(12);
        JComboBox<String> cbRol = new JComboBox<>(new String[]{"CAJERO", "ADMIN"});
        JButton btnGuardar = new JButton("💾 Guardar Usuario");
        btnGuardar.setCursor(new Cursor(Cursor.HAND_CURSOR));

        panelForm.add(new JLabel("Usuario:")); panelForm.add(txtUsuario);
        panelForm.add(new JLabel("Contraseña:")); panelForm.add(txtPass);
        panelForm.add(new JLabel("Rol:")); panelForm.add(cbRol);
        panelForm.add(btnGuardar);

        String[] cols = {"NOMBRE DE USUARIO", "NIVEL DE ACCESO (ROL)"};
        modeloUsuarios = new DefaultTableModel(cols, 0) {
            @Override

            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tablaUsuarios = new JTable(modeloUsuarios);
        tablaUsuarios.setRowHeight(30);
        tablaUsuarios.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));

        refrescarTabla();

        btnGuardar.addActionListener(e -> {
            String u = txtUsuario.getText().trim();
            String p = new String(txtPass.getPassword()).trim();
            if (u.isEmpty() || p.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El usuario y contraseña son obligatorios");
                return;
            }
            if (UsuarioDAO.registrarUsuario(u, p, cbRol.getSelectedItem().toString())) {
                JOptionPane.showMessageDialog(this, "¡Cuenta creada exitosamente!");
                txtUsuario.setText(""); txtPass.setText("");
                refrescarTabla();
            } else {
                JOptionPane.showMessageDialog(this, "El nombre de usuario ya existe.", "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        JPopupMenu popup = new JPopupMenu();
        JMenuItem itemEliminar = new JMenuItem("Eliminar Cuenta");
        popup.add(itemEliminar);
        tablaUsuarios.setComponentPopupMenu(popup);

        itemEliminar.addActionListener(e -> {
            int row = tablaUsuarios.getSelectedRow();
            if (row != -1) {
                String userAEliminar = modeloUsuarios.getValueAt(row, 0).toString();
                if (userAEliminar.equalsIgnoreCase(nombreUsuario) || userAEliminar.equalsIgnoreCase("admin")) return;
                if (JOptionPane.showConfirmDialog(this, "¿Borrar cuenta?", "Confirmar", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    UsuarioDAO.eliminarUsuario(userAEliminar);
                    refrescarTabla();
                }
            }
        });

        add(panelForm, BorderLayout.NORTH);
        add(new JScrollPane(tablaUsuarios), BorderLayout.CENTER);
    }

    private void refrescarTabla() {
        modeloUsuarios.setRowCount(0);
        for (String[] u : UsuarioDAO.obtenerTodos()) modeloUsuarios.addRow(u);
    }
}