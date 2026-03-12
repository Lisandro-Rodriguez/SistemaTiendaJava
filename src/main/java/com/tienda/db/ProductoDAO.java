package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import com.tienda.Modelo.Producto;

public class ProductoDAO {

    public static void crearTablaProductos() {
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
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement()) {
            s.execute(sql);
        } catch (SQLException e) { System.err.println("Error tabla productos: " + e.getMessage()); }
    }

    public static void registrarProducto(Producto p) {
        crearTablaProductos();
        String sql = "INSERT INTO productos(codigo_barras,nombre,tipo,marca,precio_costo,stock,margen_ganancia,precio_venta) VALUES(?,?,?,?,?,?,?,?)";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getCodigoBarras());
            ps.setString(2, p.getNombre());
            ps.setString(3, p.getTipo());
            ps.setString(4, p.getMarca());
            ps.setDouble(5, p.getPrecioCosto());
            ps.setInt(6, p.getStock());
            ps.setDouble(7, p.getMargenGanancia());
            ps.setDouble(8, p.getPrecioVenta());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error al registrar producto: " + e.getMessage()); }
    }

    public static List<Producto> obtenerTodos() {
        crearTablaProductos();
        List<Producto> lista = new ArrayList<>();
        try (Connection c = ConexionDB.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT * FROM productos ORDER BY nombre")) {
            while (rs.next())
                lista.add(new Producto(rs.getString("codigo_barras"), rs.getString("nombre"),
                        rs.getString("tipo"), rs.getString("marca"),
                        rs.getDouble("precio_costo"), rs.getInt("stock"), rs.getDouble("margen_ganancia")));
        } catch (SQLException e) { System.err.println("Error obtenerTodos: " + e.getMessage()); }
        return lista;
    }

    public static Producto buscarPorCodigo(String codigo) {
        String sql = "SELECT * FROM productos WHERE codigo_barras = ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next())
                    return new Producto(rs.getString("codigo_barras"), rs.getString("nombre"),
                            rs.getString("tipo"), rs.getString("marca"),
                            rs.getDouble("precio_costo"), rs.getInt("stock"), rs.getDouble("margen_ganancia"));
            }
        } catch (SQLException e) { System.err.println("Error buscarPorCodigo: " + e.getMessage()); }
        return null;
    }

    // FIX: stock nunca baja de 0
    public static void reducirStock(String codigo, int cantidad) {
        String sql = "UPDATE productos SET stock = MAX(0, stock - ?) WHERE codigo_barras = ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, cantidad);
            ps.setString(2, codigo);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error reducirStock: " + e.getMessage()); }
    }

    public static void eliminarProducto(String codigo) {
        String sql = "DELETE FROM productos WHERE codigo_barras = ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, codigo); ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error eliminarProducto: " + e.getMessage()); }
    }

    public static void actualizarProducto(Producto p) {
        String sql = "UPDATE productos SET nombre=?,tipo=?,marca=?,precio_costo=?,stock=?,margen_ganancia=?,precio_venta=? WHERE codigo_barras=?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, p.getNombre());  ps.setString(2, p.getTipo());
            ps.setString(3, p.getMarca());   ps.setDouble(4, p.getPrecioCosto());
            ps.setInt(5, p.getStock());      ps.setDouble(6, p.getMargenGanancia());
            ps.setDouble(7, p.getPrecioVenta()); ps.setString(8, p.getCodigoBarras());
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error actualizarProducto: " + e.getMessage()); }
    }

    public static int obtenerTotalProductosRegistrados() {
        crearTablaProductos();
        try (Connection c = ConexionDB.conectar();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM productos")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { System.err.println("Error contarProductos: " + e.getMessage()); }
        return 0;
    }

    public static int obtenerProductosBajoStock(int limite) {
        String sql = "SELECT COUNT(*) FROM productos WHERE stock <= ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) { if (rs.next()) return rs.getInt(1); }
        } catch (SQLException e) { System.err.println("Error bajStock: " + e.getMessage()); }
        return 0;
    }

    public static List<String> obtenerTipos() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT tipo FROM productos WHERE tipo IS NOT NULL AND tipo != '-' AND tipo != '' ORDER BY tipo";
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString(1));
        } catch (SQLException e) { System.err.println("Error obtenerTipos: " + e.getMessage()); }
        return lista;
    }

    public static List<String> obtenerMarcas() {
        List<String> lista = new ArrayList<>();
        String sql = "SELECT DISTINCT marca FROM productos WHERE marca IS NOT NULL AND marca != '-' AND marca != '' ORDER BY marca";
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) lista.add(rs.getString(1));
        } catch (SQLException e) { System.err.println("Error obtenerMarcas: " + e.getMessage()); }
        return lista;
    }
}