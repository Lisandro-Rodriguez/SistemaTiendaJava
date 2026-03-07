package com.tienda.db;

import com.tienda.Modelo.Producto;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProductoDAO {

    public static void crearTablaProductos() {
        // Creamos la tabla con TODOS los campos desde cero (incluyendo tipo y marca)
        String sql = "CREATE TABLE IF NOT EXISTS productos ("
                + "codigo_barras TEXT PRIMARY KEY,"
                + "nombre TEXT NOT NULL,"
                + "tipo TEXT DEFAULT '-',"
                + "marca TEXT DEFAULT '-',"
                + "precio_costo REAL NOT NULL,"
                + "stock INTEGER NOT NULL,"
                + "margen_ganancia REAL NOT NULL,"
                + "precio_venta REAL NOT NULL"
                + ");";
                
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error creando tabla productos: " + e.getMessage());
        }
    }

    public static void registrarProducto(Producto p) {
        crearTablaProductos();
        String sql = "INSERT INTO productos(codigo_barras, nombre, tipo, marca, precio_costo, stock, margen_ganancia, precio_venta) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getCodigoBarras());
            pstmt.setString(2, p.getNombre());
            pstmt.setString(3, p.getTipo());
            pstmt.setString(4, p.getMarca());
            pstmt.setDouble(5, p.getPrecioCosto());
            pstmt.setInt(6, p.getStock());
            pstmt.setDouble(7, p.getMargenGanancia());
            pstmt.setDouble(8, p.getPrecioVenta());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al registrar: " + e.getMessage());
        }
    }

    public static List<Producto> obtenerTodos() {
        crearTablaProductos();
        List<Producto> lista = new ArrayList<>();
        String sql = "SELECT * FROM productos";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Producto p = new Producto(
                        rs.getString("codigo_barras"),
                        rs.getString("nombre"),
                        rs.getString("tipo"),
                        rs.getString("marca"),
                        rs.getDouble("precio_costo"),
                        rs.getInt("stock"),
                        rs.getDouble("margen_ganancia")
                );
                lista.add(p);
            }
        } catch (SQLException e) {
            System.out.println("Error al obtener productos: " + e.getMessage());
        }
        return lista;
    }

    public static Producto buscarPorCodigo(String codigo) {
        String sql = "SELECT * FROM productos WHERE codigo_barras = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Producto(
                            rs.getString("codigo_barras"),
                            rs.getString("nombre"),
                            rs.getString("tipo"),
                            rs.getString("marca"),
                            rs.getDouble("precio_costo"),
                            rs.getInt("stock"),
                            rs.getDouble("margen_ganancia")
                    );
                }
            }
        } catch (SQLException e) {
            System.out.println("Error al buscar: " + e.getMessage());
        }
        return null;
    }

    public static void reducirStock(String codigo, int cantidadVendida) {
        String sql = "UPDATE productos SET stock = stock - ? WHERE codigo_barras = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cantidadVendida);
            pstmt.setString(2, codigo);
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public static void eliminarProducto(String codigo) {
        String sql = "DELETE FROM productos WHERE codigo_barras = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, codigo);
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public static void actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre = ?, tipo = ?, marca = ?, precio_costo = ?, stock = ?, margen_ganancia = ?, precio_venta = ? WHERE codigo_barras = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, p.getNombre());
            pstmt.setString(2, p.getTipo());
            pstmt.setString(3, p.getMarca());
            pstmt.setDouble(4, p.getPrecioCosto());
            pstmt.setInt(5, p.getStock());
            pstmt.setDouble(6, p.getMargenGanancia());
            pstmt.setDouble(7, p.getPrecioVenta());
            pstmt.setString(8, p.getCodigoBarras());
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public static int obtenerTotalProductosRegistrados() {
        String sql = "SELECT COUNT(*) as total FROM productos";
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt("total");
        } catch (SQLException e) { }
        return 0;
    }

    public static int obtenerProductosBajoStock(int limite) {
        String sql = "SELECT COUNT(*) as total FROM productos WHERE stock <= ?";
        try (Connection conn = ConexionDB.conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limite);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return rs.getInt("total");
            }
        } catch (SQLException e) { }
        return 0;
    }

    public static List<String> obtenerTipos() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT tipo FROM productos WHERE tipo IS NOT NULL AND tipo != '-' AND tipo != ''";
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString("tipo"));
        } catch (SQLException e) {}
        return lista;
    }

    public static List<String> obtenerMarcas() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT marca FROM productos WHERE marca IS NOT NULL AND marca != '-' AND marca != ''";
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString("marca"));
        } catch (SQLException e) {}
        return lista;
    }
}