package com.tienda; // o el paquete que uses
import com.tienda.vista.VentanaProducto;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VentanaProducto ventana = new VentanaProducto();
            ventana.setVisible(true);
        });
    }
}