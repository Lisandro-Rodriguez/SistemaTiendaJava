package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.prefs.Preferences;

public class PanelConfiguracion extends JPanel {

    private static final Preferences PREFS = Preferences.userRoot().node("tienda");

    private final JTextField txtNombre   = new JTextField();
    private final JTextField txtDireccion = new JTextField();
    private final JTextField txtCuit     = new JTextField();
    private final JTextField txtTelefono = new JTextField();
    private final JTextField txtEmail    = new JTextField();
    private final JTextArea  txtMensaje  = new JTextArea(2, 20);

    public PanelConfiguracion() {
        setLayout(new BorderLayout(20, 20));
        setBorder(new EmptyBorder(30, 60, 30, 60));
        construir();
    }

    private void construir() {
        // Titulo
        JLabel lblTitulo = new JLabel("Configuracion del Negocio", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(new Color(30, 90, 160));
        lblTitulo.setBorder(new EmptyBorder(0, 0, 10, 0));
        add(lblTitulo, BorderLayout.NORTH);

        // Formulario
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 210, 230), 1, true),
            new EmptyBorder(30, 40, 30, 40)));
        form.setBackground(new Color(248, 250, 255));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        Font fLabel = new Font("SansSerif", Font.BOLD, 13);
        Font fField = new Font("SansSerif", Font.PLAIN, 13);

        // Cargar valores guardados
        txtNombre.setText(PREFS.get("nombre_negocio", ""));
        txtDireccion.setText(PREFS.get("direccion", ""));
        txtCuit.setText(PREFS.get("cuit", ""));
        txtTelefono.setText(PREFS.get("telefono", ""));
        txtEmail.setText(PREFS.get("email", ""));
        txtMensaje.setText(PREFS.get("mensaje_ticket", "Gracias por su compra!"));
        txtMensaje.setLineWrap(true);
        txtMensaje.setWrapStyleWord(true);
        txtMensaje.setFont(fField);

        // Agregar campos
        agregarFila(form, gbc, 0, "Nombre del negocio:", txtNombre, fLabel, fField,
            "Ej: Kiosco La Esquina");
        agregarFila(form, gbc, 1, "Direccion:", txtDireccion, fLabel, fField,
            "Ej: Av. San Martin 123, Salta");
        agregarFila(form, gbc, 2, "CUIT:", txtCuit, fLabel, fField,
            "Ej: 20-12345678-9");
        agregarFila(form, gbc, 3, "Telefono:", txtTelefono, fLabel, fField,
            "Ej: 387-1234567");
        agregarFila(form, gbc, 4, "Email:", txtEmail, fLabel, fField,
            "Ej: kiosco@gmail.com");

        // Mensaje del ticket (textarea)
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0;
        JLabel lblMsg = new JLabel("Mensaje en ticket:");
        lblMsg.setFont(fLabel);
        form.add(lblMsg, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        JScrollPane scrollMsg = new JScrollPane(txtMensaje);
        scrollMsg.setPreferredSize(new Dimension(300, 55));
        form.add(scrollMsg, gbc);

        // Nota
        gbc.gridx = 1; gbc.gridy = 6;
        JLabel lblNota = new JLabel("Estos datos aparecen en los tickets PDF y reportes.");
        lblNota.setFont(new Font("SansSerif", Font.ITALIC, 11));
        lblNota.setForeground(new Color(120, 120, 120));
        form.add(lblNota, gbc);

        JScrollPane scroll = new JScrollPane(form);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        // Botón guardar
        JButton btnGuardar = new JButton("Guardar configuracion");
        btnGuardar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnGuardar.setBackground(new Color(39, 174, 96));
        btnGuardar.setForeground(Color.WHITE);
        btnGuardar.setFocusPainted(false);
        btnGuardar.setBorderPainted(false);
        btnGuardar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnGuardar.setPreferredSize(new Dimension(240, 44));
        btnGuardar.addActionListener(e -> guardar());

        JPanel sur = new JPanel();
        sur.setOpaque(false);
        sur.add(btnGuardar);
        add(sur, BorderLayout.SOUTH);
    }

    private void agregarFila(JPanel form, GridBagConstraints gbc, int fila,
                              String label, JTextField field, Font fLabel, Font fField,
                              String placeholder) {
        gbc.gridx = 0; gbc.gridy = fila; gbc.weightx = 0;
        JLabel lbl = new JLabel(label);
        lbl.setFont(fLabel);
        form.add(lbl, gbc);

        gbc.gridx = 1; gbc.weightx = 1;
        field.setFont(fField);
        field.setPreferredSize(new Dimension(300, 32));
        field.putClientProperty("JTextField.placeholderText", placeholder);
        form.add(field, gbc);
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre del negocio es obligatorio.",
                "Campo requerido", JOptionPane.WARNING_MESSAGE);
            txtNombre.requestFocus();
            return;
        }

        PREFS.put("nombre_negocio", nombre);
        PREFS.put("direccion",      txtDireccion.getText().trim());
        PREFS.put("cuit",           txtCuit.getText().trim());
        PREFS.put("telefono",       txtTelefono.getText().trim());
        PREFS.put("email",          txtEmail.getText().trim());
        PREFS.put("mensaje_ticket", txtMensaje.getText().trim());

        try { PREFS.flush(); } catch (Exception e) { /* ignorar */ }

        JOptionPane.showMessageDialog(this,
            "Configuracion guardada.\nLos cambios se aplican en el proximo ticket generado.",
            "Guardado", JOptionPane.INFORMATION_MESSAGE);
    }
}
