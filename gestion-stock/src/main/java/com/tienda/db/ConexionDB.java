
package com.tienda.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement; // <--- Importación nueva para ejecutar comandos SQL


/**
 * Clase encargada de gestionar la conexión a la base de datos SQLite
 * y la creación de sus tablas.
 */
public class ConexionDB {
    
    private static final String URL = "jdbc:sqlite:tienda.db";

    public static Connection conectar() {
        Connection conexion = null;
        try {
            conexion = DriverManager.getConnection(URL);
        } catch (SQLException e) {
            System.out.println("Error al conectar: " + e.getMessage());
        }
        return conexion;
    }

    /**
     * Método para crear la tabla de productos si aún no existe.
     */
    public static void crearTablaProductos() {
        // Instrucción SQL para crear la tabla
        String sql = "CREATE TABLE IF NOT EXISTS productos ("
                   + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                   + " codigo_barras TEXT NOT NULL UNIQUE,"
                   + " nombre TEXT NOT NULL,"
                   + " precio_costo REAL NOT NULL,"
                   + " precio_venta REAL NOT NULL,"
                   + " stock INTEGER NOT NULL"
                   + ");";

        // Usamos la conexión para enviar la instrucción
        try (Connection conn = conectar();
             Statement stmt = conn.createStatement()) {
            
            // Ejecutamos el comando SQL
            stmt.execute(sql);
            System.out.println("¡La tabla 'productos' está lista para usarse!");
            
        } catch (SQLException e) {
            System.out.println("Error al crear la tabla: " + e.getMessage());
        }
    }
}