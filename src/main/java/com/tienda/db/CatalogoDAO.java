package com.tienda.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Catálogo SEPA separado del inventario activo.
 * Solo se usa para autocompletar al registrar productos nuevos.
 */
public class CatalogoDAO {

    public static void crearTablaCatalogo() {
        String sql = "CREATE TABLE IF NOT EXISTS catalogo (" +
                "codigo TEXT PRIMARY KEY," +
                "nombre TEXT NOT NULL," +
                "marca TEXT DEFAULT '-'," +
                "tipo TEXT DEFAULT '-'," +
                "precio_referencia REAL DEFAULT 0" +
                ");";
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error tabla catalogo: " + e.getMessage());
        }
    }

    public static void registrarEnCatalogo(String codigo, String nombre, String marca, String tipo, double precio) {
        crearTablaCatalogo();
        String sql = "INSERT OR IGNORE INTO catalogo(codigo,nombre,marca,tipo,precio_referencia) VALUES(?,?,?,?,?)";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo);
            ps.setString(2, nombre);
            ps.setString(3, marca);
            ps.setString(4, tipo);
            ps.setDouble(5, precio);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error registrarEnCatalogo: " + e.getMessage());
        }
    }

    /** Busca en el catálogo por código — para autocompletar al escanear */
    public static String[] buscarEnCatalogo(String codigo) {
        crearTablaCatalogo();
        String sql = "SELECT nombre, marca, tipo, precio_referencia FROM catalogo WHERE codigo = ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return new String[]{
                    rs.getString("nombre"), rs.getString("marca"),
                    rs.getString("tipo"), String.valueOf(rs.getDouble("precio_referencia"))
                };
            }
        } catch (SQLException e) {
            System.err.println("Error buscarEnCatalogo: " + e.getMessage());
        }
        return null;
    }

    public static int contarProductos() {
        crearTablaCatalogo();
        try (Connection c = ConexionDB.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM catalogo")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error contarCatalogo: " + e.getMessage());
        }
        return 0;
    }

    /** Sugerencias para autocompletar por nombre */
    public static List<String[]> buscarSugerencias(String texto, int limite) {
        crearTablaCatalogo();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT codigo, nombre, marca, tipo, precio_referencia FROM catalogo " +
                     "WHERE nombre LIKE ? OR marca LIKE ? ORDER BY nombre LIMIT ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            String q = "%" + texto + "%";
            ps.setString(1, q); ps.setString(2, q); ps.setInt(3, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new String[]{
                        rs.getString("codigo"), rs.getString("nombre"),
                        rs.getString("marca"), rs.getString("tipo"),
                        String.valueOf(rs.getDouble("precio_referencia"))
                    });
            }
        } catch (SQLException e) {
            System.err.println("Error buscarSugerencias: " + e.getMessage());
        }
        return lista;
    }
}
