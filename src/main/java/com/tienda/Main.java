package com.tienda;

import javax.swing.SwingUtilities; // Importamos el login en lugar del sistema principal

import com.tienda.vista.VentanaLogin;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //la aplicación arranca pidiendo la contraseña
            VentanaLogin login = new VentanaLogin();
            login.setVisible(true);
        });
    }
}
