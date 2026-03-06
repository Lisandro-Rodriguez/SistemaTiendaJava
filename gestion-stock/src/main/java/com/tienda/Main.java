package com.tienda;

import com.tienda.db.ConexionDB;           // Importamos desde la carpeta db
import com.tienda.vista.VentanaProducto;   // Importamos desde la carpeta vista

public class Main {
    public static void main(String[] args) {
        // 1. Preparamos la base de datos
        ConexionDB.crearTablaProductos();
        
        // 2. Mostramos la ventana
        VentanaProducto miVentana = new VentanaProducto();
        miVentana.setVisible(true);
    }
}