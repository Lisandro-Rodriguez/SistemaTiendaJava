package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class VentanaLogin extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;

    public VentanaLogin() {
        setTitle("🔑 Acceso al Sistema POS");
        setSize(400, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null); // Centra la ventana en la pantalla
        setResizable(false); // No permite cambiar el tamaño

        // --- DISEÑO DEL PANEL PRINCIPAL ---
        JPanel panelPrincipal = new JPanel(new BorderLayout(10, 10));
        panelPrincipal.setBorder(new EmptyBorder(20, 30, 20, 30));

        // Título de bienvenida
        JLabel lblTitulo = new JLabel("Bienvenido al Sistema", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 20));
        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        // --- FORMULARIO (Usuario y Contraseña) ---
        JPanel panelFormulario = new JPanel(new GridLayout(2, 2, 10, 15));
        
        JLabel lblUser = new JLabel("👤 Usuario:");
        lblUser.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtUsuario = new JTextField();
        txtUsuario.setFont(new Font("SansSerif", Font.PLAIN, 14));

        JLabel lblPass = new JLabel("🔒 Contraseña:");
        lblPass.setFont(new Font("SansSerif", Font.BOLD, 14));
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 14));

        panelFormulario.add(lblUser);
        panelFormulario.add(txtUsuario);
        panelFormulario.add(lblPass);
        panelFormulario.add(txtPassword);

        panelPrincipal.add(panelFormulario, BorderLayout.CENTER);

        // --- BOTÓN DE INGRESAR ---
        JButton btnIngresar = new JButton("INGRESAR");
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnIngresar.setBackground(new Color(41, 128, 185)); // Azul elegante
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        
        // Al hacer clic o dar ENTER, ejecutamos la función de login
        btnIngresar.addActionListener(e -> intentarLogin());
        
        // Hacemos que el "Enter" en la contraseña también inicie sesión
        txtPassword.addActionListener(e -> intentarLogin());

        JPanel panelSur = new JPanel(new FlowLayout(FlowLayout.CENTER));
        panelSur.setBorder(new EmptyBorder(15, 0, 0, 0));
        panelSur.add(btnIngresar);

        panelPrincipal.add(panelSur, BorderLayout.SOUTH);

        add(panelPrincipal);
    }

    private void intentarLogin() {
        String usuario = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword()); // Extraemos la contraseña segura

        // Llamamos al "Cerebro" de la base de datos
        String rol = com.tienda.db.UsuarioDAO.autenticarUsuario(usuario, password);

        if (rol != null) { // Si entró exitosamente
            this.dispose(); // Cerramos esta ventanita de Login
            
            // Abrimos el sistema principal PASÁNDOLE EL ROL del usuario
            VentanaProducto app = new VentanaProducto(rol);
            app.setVisible(true);
        } else {
            // Si le erró al usuario o contraseña
            JOptionPane.showMessageDialog(this, 
                "Usuario o Contraseña incorrectos.", 
                "Error de Acceso", 
                JOptionPane.ERROR_MESSAGE);
        }
    }
}