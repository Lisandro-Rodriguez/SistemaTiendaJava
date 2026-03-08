package com.tienda.db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    public static void crearTablas() {
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement()) {
            
            String sqlClientes = "CREATE TABLE IF NOT EXISTS clientes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "nombre TEXT NOT NULL,"
                    + "telefono TEXT UNIQUE," // UNIQUE evita duplicados
                    + "deuda_total REAL DEFAULT 0"
                    + ");";
            stmt.execute(sqlClientes);

            String sqlMovimientos = "CREATE TABLE IF NOT EXISTS movimientos_cuenta ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "cliente_id INTEGER,"
                    + "fecha DATETIME DEFAULT CURRENT_TIMESTAMP,"
                    + "tipo TEXT," 
                    + "monto REAL,"
                    + "detalle TEXT,"
                    + "FOREIGN KEY(cliente_id) REFERENCES clientes(id)"
                    + ");";
            stmt.execute(sqlMovimientos);
            
        } catch (SQLException e) {}
    }

    public static boolean registrarCliente(String nombre, String telefono) {
        crearTablas();
        // Verificar si el teléfono ya existe
        if (!telefono.isEmpty() && telefonoExiste(telefono)) return false;

        String sql = "INSERT INTO clientes(nombre, telefono) VALUES(?,?)";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre);
            pstmt.setString(2, telefono);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) { return false; }
    }

    private static boolean telefonoExiste(String telefono) {
        String sql = "SELECT COUNT(*) FROM clientes WHERE telefono = ?";
        try (Connection conn = ConexionDB.conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)){
            pstmt.setString(1, telefono);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {}
        return false;
    }

    public static List<String[]> obtenerClientes() {
        crearTablas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, nombre, telefono, deuda_total FROM clientes ORDER BY nombre";
        try (Connection conn = ConexionDB.conectar();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString("nombre"),
                    rs.getString("telefono") == null ? "" : rs.getString("telefono"),
                    String.format("%.2f", rs.getDouble("deuda_total"))
                });
            }
        } catch (SQLException e) {}
        return lista;
    }

    public static void registrarMovimiento(int clienteId, String tipo, double monto, String detalle) {
        String sqlInsert = "INSERT INTO movimientos_cuenta(cliente_id, tipo, monto, detalle) VALUES(?,?,?,?)";
        String sqlUpdateDebt = "UPDATE clientes SET deuda_total = deuda_total + ? WHERE id = ?";
        double ajusteDeuda = tipo.equals("FIADO") ? monto : -monto;

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert);
             PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateDebt)) {
            pstmtInsert.setInt(1, clienteId); pstmtInsert.setString(2, tipo); pstmtInsert.setDouble(3, monto); pstmtInsert.setString(4, detalle);
            pstmtInsert.executeUpdate();
            pstmtUpdate.setDouble(1, ajusteDeuda); pstmtUpdate.setInt(2, clienteId);
            pstmtUpdate.executeUpdate();
        } catch (SQLException e) {}
    }

    public static List<String[]> obtenerHistorialCliente(int clienteId) {
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT datetime(fecha, 'localtime') as fecha_local, tipo, monto, detalle " +
                     "FROM movimientos_cuenta WHERE cliente_id = ? ORDER BY id DESC";
        try (Connection conn = ConexionDB.conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, clienteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(new String[]{
                        rs.getString("fecha_local"), rs.getString("tipo"), "$" + String.format("%.2f", rs.getDouble("monto")), rs.getString("detalle")
                    });
                }
            }
        } catch (SQLException e) {}
        return lista;
    }

    // NUEVO: Para editar
    public static boolean actualizarCliente(int id, String nombre, String telefono) {
        String sql = "UPDATE clientes SET nombre = ?, telefono = ? WHERE id = ?";
        try (Connection conn = ConexionDB.conectar(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, nombre); pstmt.setString(2, telefono); pstmt.setInt(3, id);
            pstmt.executeUpdate(); return true;
        } catch (SQLException e) { return false; }
    }

    // NUEVO: Para eliminar
    public static void eliminarCliente(int id) {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement p1 = conn.prepareStatement("DELETE FROM movimientos_cuenta WHERE cliente_id = ?");
             PreparedStatement p2 = conn.prepareStatement("DELETE FROM clientes WHERE id = ?")) {
            p1.setInt(1, id); p1.executeUpdate();
            p2.setInt(1, id); p2.executeUpdate();
        } catch (SQLException e) {}
    }
}