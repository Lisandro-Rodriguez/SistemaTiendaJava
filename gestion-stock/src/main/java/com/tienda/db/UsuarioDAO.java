package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UsuarioDAO{

    public static void crearTablaUsuarios() {
        String sql = "CREATE TABLE IF NOT EXISTS usuarios ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT UNIQUE NOT NULL,"
                + " password TEXT NOT NULL,"
                + " rol TEXT NOT NULL"
                + ");";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            insertarUsuariosPorDefecto(); // Agregamos los usuarios de prueba automáticamente
        } catch (SQLException e) {
            System.out.println("Error creando tabla usuarios: " + e.getMessage());
        }
    }

     //Admin y un Cajero de prueba si la tabla está vacía
    
    private static void insertarUsuariosPorDefecto() {
        String sqlCheck = "SELECT COUNT(*) AS total FROM usuarios";
        String sqlInsert = "INSERT INTO usuarios (username, password, rol) VALUES (?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {
            
            if (rs.next() && rs.getInt("total") == 0) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                    // Creamos el usuario Dueño/Admin
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, "admin123");
                    pstmt.setString(3, "ADMIN");
                    pstmt.executeUpdate();

                    // Creamos el usuario Empleado/Cajero
                    pstmt.setString(1, "cajero");
                    pstmt.setString(2, "cajero123");
                    pstmt.setString(3, "CAJERO");
                    pstmt.executeUpdate();
                    
                    System.out.println("Usuarios por defecto creados con éxito.");
                }
            }
        } catch (SQLException e) {
            System.out.println("Error verificando usuarios por defecto: " + e.getMessage());
        }
    }

    // verificar si el usuario y contraseña son correctos
    // Retorna el ROL ("ADMIN" o "CAJERO") si es exitoso, o null si falla.
    public static String autenticarUsuario(String username, String password) {
        crearTablaUsuarios(); // Nos aseguramos de que la tabla exista antes de buscar
        String sql = "SELECT rol FROM usuarios WHERE username = ? AND password = ?";
        
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, username);
            pstmt.setString(2, password);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("rol"); // Login exitoso
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al autenticar: " + e.getMessage());
        }
        return null; // Login fallido
    }
    }

