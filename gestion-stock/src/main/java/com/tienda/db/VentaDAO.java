package com.tienda.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    public static void crearTablaVentas() {
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS ventas ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "fecha DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "detalle TEXT NOT NULL,"
                    + "total REAL NOT NULL,"
                    + "metodo_pago TEXT NOT NULL"
                    + ");";
            stmt.execute(sql);

            // MAGIA: Agregar columna cajero sin borrar las ventas viejas
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN cajero TEXT DEFAULT 'Admin'"); } catch(Exception e) {}
        } catch (SQLException e) {}
    }

    public static void registrarVenta(String detalle, double total, String metodo, String cajero) {
        crearTablaVentas();
        String sql = "INSERT INTO ventas(detalle, total, metodo_pago, cajero) VALUES(?,?,?,?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, detalle);
            pstmt.setDouble(2, total);
            pstmt.setString(3, metodo);
            pstmt.setString(4, cajero);
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public static List<String[]> obtenerHistorial() {
        crearTablaVentas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, datetime(fecha, 'localtime') as fecha_local, detalle, total, metodo_pago, cajero FROM ventas ORDER BY id DESC";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            // Verificamos que exista la columna para no romper el programa
            ResultSetMetaData meta = rs.getMetaData();
            boolean tieneCajero = false;
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                if (meta.getColumnName(i).equalsIgnoreCase("cajero")) tieneCajero = true;
            }

            while (rs.next()) {
                String cajero = tieneCajero ? rs.getString("cajero") : "Admin";
                if (cajero == null) cajero = "Admin";
                
                lista.add(new String[]{
                        String.valueOf(rs.getInt("id")),
                        rs.getString("fecha_local"),
                        rs.getString("detalle"),
                        "$" + String.format("%.2f", rs.getDouble("total")),
                        rs.getString("metodo_pago"),
                        cajero // ¡La nueva columna!
                });
            }
        } catch (SQLException e) {}
        return lista;
    }
    
    public static double obtenerTotalVentasHistorico() {
        crearTablaVentas();
        String sql = "SELECT SUM(total) as total FROM ventas";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) { }
        return 0;
    }
}