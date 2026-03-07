package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    // 1. Crea la tabla si no existe
    public static void crearTablaVentas() {
        String sql = "CREATE TABLE IF NOT EXISTS ventas ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " fecha DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + " detalle TEXT,"
                + " total REAL,"
                + " metodo_pago TEXT"
                + ");";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.out.println("Error creando tabla ventas: " + e.getMessage());
        }
    }

    // 2. Guarda una nueva venta
    public static void registrarVenta(String detalle, double total, String metodoPago) {
        crearTablaVentas(); // Nos aseguramos de que exista
        String sql = "INSERT INTO ventas(detalle, total, metodo_pago) VALUES(?,?,?)";
        
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, detalle);
            pstmt.setDouble(2, total);
            pstmt.setString(3, metodoPago);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error al registrar venta: " + e.getMessage());
        }
    }

    // 3. Obtiene todas las ventas para mostrarlas en el historial
    public static List<String[]> obtenerHistorial() {
        crearTablaVentas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, datetime(fecha, 'localtime') as fecha_local, detalle, total, metodo_pago FROM ventas ORDER BY id DESC";
        
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("fecha_local"),
                    rs.getString("detalle"),
                    "$" + String.format("%.2f", rs.getDouble("total")),
                    rs.getString("metodo_pago")
                });
            }
        } catch (SQLException e) {
            System.out.println("Error obteniendo historial: " + e.getMessage());
        }
        return lista;
    }

    // Obtener la suma total de dinero vendido históricamente
    public static double obtenerTotalVentasHistorico() {
        crearTablaVentas();
        String sql = "SELECT SUM(total) as gran_total FROM ventas";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("gran_total");
        } catch (SQLException e) {
            System.out.println("Error calculando total: " + e.getMessage());
        }
        return 0.0;
    }
}