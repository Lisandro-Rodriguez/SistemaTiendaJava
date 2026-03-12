package com.tienda;

import javax.swing.SwingUtilities;
import com.tienda.util.ActualizadorApp;
import com.tienda.vista.VentanaLogin;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaLogin login = new VentanaLogin();
            login.setVisible(true);
            // Verificar actualizaciones 3 segundos despues de arrancar (no bloqueante)
            javax.swing.Timer t = new javax.swing.Timer(3000, e ->
                ActualizadorApp.verificarEnSegundoPlano(login));
            t.setRepeats(false);
            t.start();
        });
    }
}
