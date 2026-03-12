package com.tienda.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

import com.formdev.flatlaf.FlatLightLaf;
import com.tienda.db.UsuarioDAO;

public class VentanaLogin extends JFrame {

    private JTextField txtUsuario;
    private final JPasswordField txtPassword;
    private final JButton btnIngresar;
    private final JLabel lblError;

    public VentanaLogin() {
        try { UIManager.setLookAndFeel(new FlatLightLaf()); } catch (Exception ex) {}

        setTitle("Sistema POS — Inicio de Sesión");
        setSize(440, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(245, 247, 250));

        JPanel header = new JPanel(new GridLayout(3, 1, 0, 6));
        header.setBackground(new Color(30, 90, 160));
        header.setBorder(new EmptyBorder(38, 40, 38, 40));
        JLabel ico = new JLabel("🏪", SwingConstants.CENTER);
        ico.setFont(new Font("SansSerif", Font.PLAIN, 50));
        JLabel titulo = new JLabel("Sistema POS", SwingConstants.CENTER);
        titulo.setFont(new Font("SansSerif", Font.BOLD, 27));
        titulo.setForeground(Color.WHITE);
        JLabel sub = new JLabel("Gestión de Tienda · Argentina", SwingConstants.CENTER);
        sub.setFont(new Font("SansSerif", Font.PLAIN, 13));
        sub.setForeground(new Color(180, 210, 255));
        header.add(ico); header.add(titulo); header.add(sub);
        root.add(header, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setBackground(new Color(245, 247, 250));
        form.setBorder(new EmptyBorder(32, 45, 20, 45));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(5, 0, 5, 0);

        Font labelFont = new Font("SansSerif", Font.BOLD, 13);
        Font fieldFont = new Font("SansSerif", Font.PLAIN, 15);
        Color labelColor = new Color(70, 80, 100);

        JLabel lUser = new JLabel("Usuario");
        lUser.setFont(labelFont); lUser.setForeground(labelColor);
        txtUsuario = new JTextField();
        txtUsuario.setFont(fieldFont);
        txtUsuario.setPreferredSize(new Dimension(0, 44));
        txtUsuario.putClientProperty("JTextField.placeholderText", "Ingresá tu usuario");

        JLabel lPass = new JLabel("Contraseña");
        lPass.setFont(labelFont); lPass.setForeground(labelColor);
        txtPassword = new JPasswordField();
        txtPassword.setFont(fieldFont);
        txtPassword.setPreferredSize(new Dimension(0, 44));
        txtPassword.putClientProperty("JTextField.placeholderText", "Ingresá tu contraseña");

        lblError = new JLabel(" ", SwingConstants.CENTER);
        lblError.setFont(new Font("SansSerif", Font.BOLD, 12));
        lblError.setForeground(new Color(220, 53, 69));

        btnIngresar = new JButton("INGRESAR");
        btnIngresar.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnIngresar.setPreferredSize(new Dimension(0, 48));
        btnIngresar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnIngresar.setBackground(new Color(30, 90, 160));
        btnIngresar.setForeground(Color.WHITE);
        btnIngresar.setFocusPainted(false);
        btnIngresar.setBorderPainted(false);

        g.gridy = 0; form.add(lUser, g);
        g.gridy = 1; form.add(txtUsuario, g);
        g.gridy = 2; g.insets = new Insets(16, 0, 5, 0); form.add(lPass, g);
        g.gridy = 3; g.insets = new Insets(5, 0, 5, 0); form.add(txtPassword, g);
        g.gridy = 4; form.add(lblError, g);
        g.gridy = 5; g.insets = new Insets(12, 0, 0, 0); form.add(btnIngresar, g);
        root.add(form, BorderLayout.CENTER);

        JLabel ver = new JLabel("v1.0  —  Sistema POS", SwingConstants.CENTER);
        ver.setFont(new Font("SansSerif", Font.PLAIN, 11));
        ver.setForeground(new Color(180, 185, 195));
        ver.setBorder(new EmptyBorder(0, 0, 16, 0));
        root.add(ver, BorderLayout.SOUTH);
        add(root);

        btnIngresar.addActionListener(e -> login());
        KeyAdapter enter = new KeyAdapter() {
            @Override

            public void keyPressed(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) login(); }
        };
        txtUsuario.addKeyListener(enter);
        txtPassword.addKeyListener(enter);
        SwingUtilities.invokeLater(() -> txtUsuario.requestFocus());
    }

    private void login() {
        String user = txtUsuario.getText().trim();
        String pass = new String(txtPassword.getPassword()).trim();
        if (user.isEmpty() || pass.isEmpty()) { mostrarError("Completá usuario y contraseña."); return; }
        btnIngresar.setEnabled(false);
        btnIngresar.setText("Verificando...");
        lblError.setText(" ");
        new Thread(() -> {
            String rol = UsuarioDAO.autenticarUsuario(user, pass);
            SwingUtilities.invokeLater(() -> {
                btnIngresar.setEnabled(true);
                btnIngresar.setText("INGRESAR");
                if (rol != null) { dispose(); new VentanaProducto(rol, user).setVisible(true); }
                else { mostrarError("Usuario o contraseña incorrectos."); txtPassword.setText(""); txtPassword.requestFocus(); }
            });
        }).start();
    }

    private void mostrarError(String msg) {
        lblError.setText(msg);
        Timer t = new Timer(3500, e -> lblError.setText(" "));
        t.setRepeats(false); t.start();
    }
}