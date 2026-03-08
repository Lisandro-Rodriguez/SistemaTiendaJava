package com.tienda.vista;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import com.tienda.db.UsuarioDAO;
import com.formdev.flatlaf.FlatLightLaf;

public class VentanaLogin extends JFrame {

    private JTextField txtUsuario;
    private JPasswordField txtPassword;
    private JButton btnIngresar;

    public VentanaLogin() {
        // Asegurarnos de que el diseño moderno esté cargado desde el principio
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (Exception ex) {
            System.err.println("No se pudo inicializar FlatLaf en el Login");
        }

        setTitle("Inicio de Sesión - Sistema POS");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // Panel Principal
        JPanel panelPrincipal = new JPanel(new BorderLayout());
        panelPrincipal.setBorder(new EmptyBorder(30, 40, 30, 40));

        // Título / Logo
        JLabel lblTitulo = new JLabel("Bienvenido", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 28));
        lblTitulo.setBorder(new EmptyBorder(0, 0, 30, 0));
        panelPrincipal.add(lblTitulo, BorderLayout.NORTH);

        // Formulario Central
        JPanel panelFormulario = new JPanel(new GridLayout(4, 1, 10, 10));
        
        JLabel lblUsuario = new JLabel("Usuario:");
        lblUsuario.setFont(new Font("SansSerif", Font.PLAIN, 16));
        txtUsuario = new JTextField();
        txtUsuario.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JLabel lblPass = new JLabel("Contraseña:");
        lblPass.setFont(new Font("SansSerif", Font.PLAIN, 16));
        txtPassword = new JPasswordField();
        txtPassword.setFont(new Font("SansSerif", Font.PLAIN, 16));

        panelFormulario.add(lblUsuario);
        panelFormulario.add(txtUsuario);
        panelFormulario.add(lblPass);
        panelFormulario.add(txtPassword);

        panelPrincipal.add(panelFormulario, BorderLayout.CENTER);

        // Botón Inferior
        JPanel panelBotones = new JPanel(new BorderLayout());
        panelBotones.setBorder(new EmptyBorder(30, 0, 0, 0));
        
        btnIngresar = new JButton("INGRESAR");
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 16));
        btnIngresar.setPreferredSize(new Dimension(100, 45));
        btnIngresar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnIngresar.setBackground(new Color(52, 152, 219));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        
        btnIngresar.addActionListener(e -> iniciarSesion());
        
        panelBotones.add(btnIngresar, BorderLayout.CENTER);
        panelPrincipal.add(panelBotones, BorderLayout.SOUTH);

        // Magia UX: Presionar ENTER para iniciar sesión
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    iniciarSesion();
                }
            }
        };
        txtUsuario.addKeyListener(enterListener);
        txtPassword.addKeyListener(enterListener);

        add(panelPrincipal);
    }

    private void iniciarSesion() {
        String username = txtUsuario.getText().trim();
        String password = new String(txtPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Por favor, complete todos los campos.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // ¡AQUÍ CONSULTAMOS A LA BASE DE DATOS REAL!
        String rol = UsuarioDAO.autenticarUsuario(username, password);

        if (rol != null) {
            // Cerramos el login
            this.dispose();
            
            // ¡MAGIA!: Abrimos la ventana principal, pasándole el ROL y el NOMBRE DE USUARIO
            SwingUtilities.invokeLater(() -> {
                new VentanaProducto(rol, username).setVisible(true);
            });
            
        } else {
            JOptionPane.showMessageDialog(this, "Usuario o contraseña incorrectos.", "Error de Autenticación", JOptionPane.ERROR_MESSAGE);
            txtPassword.setText(""); // Limpiamos la contraseña por seguridad
            txtPassword.requestFocus();
        }
    }
}