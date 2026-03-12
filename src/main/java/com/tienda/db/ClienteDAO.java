package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public static void crearTablas() {
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement()) {
            s.execute("CREATE TABLE IF NOT EXISTS clientes (id INTEGER PRIMARY KEY AUTOINCREMENT, nombre TEXT NOT NULL, telefono TEXT UNIQUE, deuda_total REAL DEFAULT 0);");
            s.execute("CREATE TABLE IF NOT EXISTS movimientos_cuenta (id INTEGER PRIMARY KEY AUTOINCREMENT, cliente_id INTEGER, fecha DATETIME DEFAULT CURRENT_TIMESTAMP, tipo TEXT, monto REAL, detalle TEXT, FOREIGN KEY(cliente_id) REFERENCES clientes(id));");
            try { s.execute("ALTER TABLE movimientos_cuenta ADD COLUMN estado_caja TEXT DEFAULT 'ABIERTA'"); } catch (Exception e) {}
        } catch (SQLException e) { System.err.println("Error tablas clientes: " + e.getMessage()); }
    }

    public static boolean registrarCliente(String nombre, String telefono) {
        crearTablas();
        // Si el teléfono está vacío lo ponemos null para evitar conflictos UNIQUE
        String tel = (telefono == null || telefono.trim().isEmpty()) ? null : telefono.trim();
        if (tel != null && telefonoExiste(tel)) return false;
        String sql = "INSERT INTO clientes(nombre, telefono) VALUES(?,?)";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            if (tel != null) ps.setString(2, tel); else ps.setNull(2, Types.VARCHAR);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Error registrarCliente: " + e.getMessage());
            return false;
        }
    }

    private static boolean telefonoExiste(String telefono) {
        String sql = "SELECT COUNT(*) FROM clientes WHERE telefono = ?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, telefono);
            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) { return false; }
    }

    public static List<String[]> obtenerClientes() {
        crearTablas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, telefono, deuda_total FROM clientes ORDER BY nombre";
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                lista.add(new String[]{String.valueOf(rs.getInt("id")), rs.getString("nombre"),
                        rs.getString("telefono") == null ? "" : rs.getString("telefono"),
                        String.format("%.2f", rs.getDouble("deuda_total"))});
        } catch (SQLException e) { System.err.println("Error obtenerClientes: " + e.getMessage()); }
        return lista;
    }

    public static void registrarMovimiento(int clienteId, String tipo, double monto, String detalle) {
        String sqlI = "INSERT INTO movimientos_cuenta(cliente_id,tipo,monto,detalle) VALUES(?,?,?,?)";
        String sqlU = "UPDATE clientes SET deuda_total = deuda_total + ? WHERE id = ?";
        double ajuste = tipo.equals("FIADO") ? monto : -monto;
        try (Connection c = ConexionDB.conectar();
             PreparedStatement pi = c.prepareStatement(sqlI);
             PreparedStatement pu = c.prepareStatement(sqlU)) {
            pi.setInt(1, clienteId); pi.setString(2, tipo);
            pi.setDouble(3, monto);  pi.setString(4, detalle);
            pi.executeUpdate();
            pu.setDouble(1, ajuste); pu.setInt(2, clienteId);
            pu.executeUpdate();
        } catch (SQLException e) { System.err.println("Error registrarMovimiento: " + e.getMessage()); }
    }

    public static List<String[]> obtenerHistorialCliente(int clienteId) {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT datetime(fecha,'localtime'), tipo, monto, detalle FROM movimientos_cuenta WHERE cliente_id=? ORDER BY id DESC";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, clienteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next())
                    lista.add(new String[]{rs.getString(1), rs.getString(2),
                            "$" + String.format("%.2f", rs.getDouble(3)), rs.getString(4)});
            }
        } catch (SQLException e) { System.err.println("Error historialCliente: " + e.getMessage()); }
        return lista;
    }

    // FIX: captura correctamente la excepción de UNIQUE constraint
    public static boolean actualizarCliente(int id, String nombre, String telefono) {
        String tel = (telefono == null || telefono.trim().isEmpty()) ? null : telefono.trim();
        String sql = "UPDATE clientes SET nombre=?, telefono=? WHERE id=?";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, nombre.trim());
            if (tel != null) ps.setString(2, tel); else ps.setNull(2, Types.VARCHAR);
            ps.setInt(3, id);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // SQLite lanza código 19 o 2067 para UNIQUE constraint
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique")) return false;
            System.err.println("Error actualizarCliente: " + e.getMessage());
            return false;
        }
    }

    public static void eliminarCliente(int id) {
        try (Connection c = ConexionDB.conectar();
             PreparedStatement p1 = c.prepareStatement("DELETE FROM movimientos_cuenta WHERE cliente_id=?");
             PreparedStatement p2 = c.prepareStatement("DELETE FROM clientes WHERE id=?")) {
            p1.setInt(1, id); p1.executeUpdate();
            p2.setInt(1, id); p2.executeUpdate();
        } catch (SQLException e) { System.err.println("Error eliminarCliente: " + e.getMessage()); }
    }

    public static double obtenerTotalPagosCajaAbierta() {
        crearTablas();
        String sql = "SELECT SUM(monto) FROM movimientos_cuenta WHERE tipo='PAGO' AND estado_caja='ABIERTA'";
        try (Connection c = ConexionDB.conectar(); Statement s = c.createStatement(); ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("Error pagosCajaAbierta: " + e.getMessage()); }
        return 0;
    }

    public static void cerrarPagosCaja() {
        String sql = "UPDATE movimientos_cuenta SET estado_caja='CERRADA' WHERE estado_caja='ABIERTA'";
        try (Connection c = ConexionDB.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error cerrarPagosCaja: " + e.getMessage()); }
    }
}