package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.mindrot.jbcrypt.BCrypt;

public class UsuarioDAO {

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
            insertarUsuariosPorDefecto();
        } catch (SQLException e) {
            System.err.println("Error creando tabla usuarios: " + e.getMessage());
        }
    }

    private static void insertarUsuariosPorDefecto() {
        String sqlCheck = "SELECT COUNT(*) AS total FROM usuarios";
        String sqlInsert = "INSERT INTO usuarios (username, password, rol) VALUES (?, ?, ?)";

        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sqlCheck)) {

            if (rs.next() && rs.getInt("total") == 0) {
                try (PreparedStatement pstmt = conn.prepareStatement(sqlInsert)) {
                    // Contraseñas hasheadas con BCrypt
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, BCrypt.hashpw("admin123", BCrypt.gensalt()));
                    pstmt.setString(3, "ADMIN");
                    pstmt.executeUpdate();

                    pstmt.setString(1, "cajero");
                    pstmt.setString(2, BCrypt.hashpw("cajero123", BCrypt.gensalt()));
                    pstmt.setString(3, "CAJERO");
                    pstmt.executeUpdate();

                    System.out.println("Usuarios por defecto creados con exito.");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error verificando usuarios por defecto: " + e.getMessage());
        }
    }

    public static String autenticarUsuario(String username, String password) {
        crearTablaUsuarios();
        String sql = "SELECT password, rol FROM usuarios WHERE username = ?";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashGuardado = rs.getString("password");
                    // Verificamos la contraseña contra el hash
                    if (BCrypt.checkpw(password, hashGuardado)) {
                        return rs.getString("rol");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error al autenticar: " + e.getMessage());
        }
        return null;
    }

    public static boolean registrarUsuario(String username, String password, String rol) {
        crearTablaUsuarios();
        String sql = "INSERT INTO usuarios(username, password, rol) VALUES(?,?,?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, BCrypt.hashpw(password, BCrypt.gensalt()));
            pstmt.setString(3, rol);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error al registrar usuario: " + e.getMessage());
            return false;
        }
    }

    public static List<String[]> obtenerTodos() {
        crearTablaUsuarios();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT username, rol FROM usuarios";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new String[]{rs.getString("username"), rs.getString("rol")});
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener usuarios: " + e.getMessage());
        }
        return lista;
    }

    public static void eliminarUsuario(String username) {
        String sql = "DELETE FROM usuarios WHERE username = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al eliminar usuario: " + e.getMessage());
        }
    }
}