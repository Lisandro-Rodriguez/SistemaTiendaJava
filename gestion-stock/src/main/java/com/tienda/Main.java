package com.tienda;

import com.tienda.vista.VentanaLogin; // Importamos el login en lugar del sistema principal
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            //la aplicación arranca pidiendo la contraseña
            VentanaLogin login = new VentanaLogin();
            login.setVisible(true);
        });
    }
}