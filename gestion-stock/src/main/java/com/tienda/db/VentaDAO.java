package com.tienda.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class VentaDAO {

    public static void crearTablaVentas() {
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS ventas ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "fecha DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "detalle TEXT NOT NULL,"
                    + "total REAL NOT NULL,"
                    + "metodo_pago TEXT NOT NULL"
                    + ");";
            stmt.execute(sql);

            // Agregamos columnas sin romper ventas viejas
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN cajero TEXT DEFAULT 'Admin'"); } catch(Exception e) {}
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN cliente TEXT DEFAULT '-'"); } catch(Exception e) {}
        } catch (SQLException e) {}
    }

    // AHORA RECIBE EL CLIENTE
    public static void registrarVenta(String detalle, double total, String metodo, String cajero, String cliente) {
        crearTablaVentas();
        String sql = "INSERT INTO ventas(detalle, total, metodo_pago, cajero, cliente) VALUES(?,?,?,?,?)";
        try (Connection conn = ConexionDB.conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, detalle); pstmt.setDouble(2, total); pstmt.setString(3, metodo); 
            pstmt.setString(4, cajero); pstmt.setString(5, cliente);
            pstmt.executeUpdate();
        } catch (SQLException e) {}
    }

    public static List<String[]> obtenerHistorial() {
        crearTablaVentas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, datetime(fecha, 'localtime') as fecha_local, detalle, total, metodo_pago, cajero, cliente FROM ventas ORDER BY id DESC";
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            
            ResultSetMetaData meta = rs.getMetaData();
            boolean tieneCliente = false;
            for (int i = 1; i <= meta.getColumnCount(); i++) if (meta.getColumnName(i).equalsIgnoreCase("cliente")) tieneCliente = true;

            while (rs.next()) {
                String c = rs.getString("cajero");
                String cli = tieneCliente ? rs.getString("cliente") : "-";
                
                lista.add(new String[]{
                        String.valueOf(rs.getInt("id")), rs.getString("fecha_local"),
                        cli == null ? "-" : cli, // Columna de CLIENTE nueva
                        rs.getString("detalle"), "$" + String.format("%.2f", rs.getDouble("total")),
                        rs.getString("metodo_pago"), c == null ? "Admin" : c
                });
            }
        } catch (SQLException e) {}
        return lista;
    }
    
    public static double obtenerTotalVentasHistorico() {
        crearTablaVentas();
        String sql = "SELECT SUM(total) as total FROM ventas";
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble("total");
        } catch (SQLException e) { }
        return 0;
    }
}