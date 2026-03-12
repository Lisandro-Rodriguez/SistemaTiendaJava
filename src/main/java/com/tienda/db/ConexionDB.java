package com.tienda.db;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionDB {

    private static final String DB_NAME = "tienda.db";

    private static String obtenerRutaDB() {
        String appData = System.getenv("APPDATA");
        if (appData == null) {
            appData = System.getProperty("user.home");
        }
        File carpeta = new File(appData, "TiendaStock");
        if (!carpeta.exists()) {
            carpeta.mkdirs();
        }
        return new File(carpeta, DB_NAME).getAbsolutePath();
    }

    public static Connection conectar() {
        Connection conexion = null;
        try {
            String url = "jdbc:sqlite:" + obtenerRutaDB();
            conexion = DriverManager.getConnection(url);
        } catch (SQLException e) {
            System.err.println("Error al conectar: " + e.getMessage());
        }
        return conexion;
    }
}