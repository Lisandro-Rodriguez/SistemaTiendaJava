package com.tienda.db;

import java.sql.*;
import java.util.*;

public class VentaDAO {

    public static void crearTablaVentas() {
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS ventas (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "fecha DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "detalle TEXT NOT NULL," +
                    "total REAL NOT NULL," +
                    "metodo_pago TEXT NOT NULL" +
                    ");");
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN cajero TEXT DEFAULT 'Admin'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN cliente TEXT DEFAULT '-'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN estado_caja TEXT DEFAULT 'ABIERTA'"); } catch (Exception e) {}
            try { stmt.execute("ALTER TABLE ventas ADD COLUMN costo_total REAL DEFAULT 0"); } catch (Exception e) {}
        } catch (SQLException e) {
            System.err.println("Error creando tabla ventas: " + e.getMessage());
        }
    }

    public static void crearTablaCierres() {
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS cierres_caja (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "fecha DATETIME DEFAULT CURRENT_TIMESTAMP," +
                    "cajero TEXT NOT NULL," +
                    "fondo_inicial REAL NOT NULL," +
                    "ventas_efectivo REAL NOT NULL," +
                    "pagos_fiado REAL NOT NULL," +
                    "ventas_digitales REAL NOT NULL," +
                    "ventas_fiadas REAL DEFAULT 0," +
                    "total_facturado REAL NOT NULL" +
                    ");");
            try { stmt.execute("ALTER TABLE cierres_caja ADD COLUMN ventas_fiadas REAL DEFAULT 0"); } catch (Exception e) {}
        } catch (SQLException e) {
            System.err.println("Error creando tabla cierres: " + e.getMessage());
        }
    }

    /** Registra venta con costo_total para calcular ganancias */
    public static void registrarVenta(String detalle, double total, String metodo, String cajero, String cliente) {
        registrarVentaConCosto(detalle, total, 0, metodo, cajero, cliente);
    }

    public static void registrarVentaConCosto(String detalle, double total, double costoTotal, String metodo, String cajero, String cliente) {
        crearTablaVentas();
        String sql = "INSERT INTO ventas(detalle,total,costo_total,metodo_pago,cajero,cliente) VALUES(?,?,?,?,?,?)";
        try (Connection conn = ConexionDB.conectar(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, detalle);
            ps.setDouble(2, total);
            ps.setDouble(3, costoTotal);
            ps.setString(4, metodo);
            ps.setString(5, cajero);
            ps.setString(6, cliente);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al registrar venta: " + e.getMessage());
        }
    }

    public static List<String[]> obtenerHistorial() {
        crearTablaVentas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, datetime(fecha,'localtime') as fecha_local, detalle, total, metodo_pago, cajero, cliente FROM ventas ORDER BY id DESC";
        try (Connection conn = ConexionDB.conectar(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                String c = rs.getString("cajero");
                String cli = rs.getString("cliente");
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("fecha_local"),
                    cli == null ? "-" : cli, rs.getString("detalle"),
                    "$" + String.format("%.2f", rs.getDouble("total")),
                    rs.getString("metodo_pago"), c == null ? "Admin" : c
                });
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener historial: " + e.getMessage());
        }
        return lista;
    }

    public static double obtenerTotalVentasHistorico() {
        crearTablaVentas();
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COALESCE(SUM(total),0) FROM ventas")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("Error total ventas: " + e.getMessage()); }
        return 0;
    }

    public static double obtenerCostoTotalHistorico() {
        crearTablaVentas();
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COALESCE(SUM(costo_total),0) FROM ventas")) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("Error costo ventas: " + e.getMessage()); }
        return 0;
    }

    /** Ganancia del día actual */
    public static double[] obtenerVentasYGananciaHoy() {
        crearTablaVentas();
        String sql = "SELECT COALESCE(SUM(total),0), COALESCE(SUM(costo_total),0) FROM ventas " +
                     "WHERE date(fecha,'localtime') = date('now','localtime')";
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                double ventas = rs.getDouble(1);
                double costo  = rs.getDouble(2);
                return new double[]{ventas, ventas - costo};
            }
        } catch (SQLException e) { System.err.println("Error ventas hoy: " + e.getMessage()); }
        return new double[]{0, 0};
    }

    /** Ganancia por período: "hoy", "semana", "mes", "historico" */
    public static double[] obtenerVentasYGananciaPeriodo(String periodo) {
        crearTablaVentas();
        String filtro = switch (periodo) {
            case "hoy"      -> "date(fecha,'localtime') = date('now','localtime')";
            case "semana"   -> "fecha >= datetime('now','-7 days')";
            case "mes"      -> "fecha >= datetime('now','-30 days')";
            default         -> "1=1";
        };
        String sql = "SELECT COALESCE(SUM(total),0), COALESCE(SUM(costo_total),0) FROM ventas WHERE " + filtro;
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            if (rs.next()) {
                double ventas = rs.getDouble(1);
                double costo  = rs.getDouble(2);
                return new double[]{ventas, ventas - costo};
            }
        } catch (SQLException e) { System.err.println("Error ventas periodo: " + e.getMessage()); }
        return new double[]{0, 0};
    }

    /** Datos por día de los últimos N días, para gráficos */
    public static List<String[]> obtenerVentasPorDia(int dias) {
        crearTablaVentas();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT date(fecha,'localtime') as dia, " +
                     "COALESCE(SUM(total),0) as total, COALESCE(SUM(costo_total),0) as costo " +
                     "FROM ventas WHERE fecha >= datetime('now','-" + dias + " days') " +
                     "GROUP BY dia ORDER BY dia";
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                double t = rs.getDouble("total");
                double c = rs.getDouble("costo");
                lista.add(new String[]{rs.getString("dia"),
                    String.format("%.2f", t), String.format("%.2f", t - c)});
            }
        } catch (SQLException e) { System.err.println("Error ventas por dia: " + e.getMessage()); }
        return lista;
    }

    public static Map<String, Double> obtenerTotalesCajaAbierta() {
        crearTablaVentas();
        Map<String, Double> totales = new HashMap<>();
        String sql = "SELECT metodo_pago, SUM(total) as suma FROM ventas WHERE estado_caja='ABIERTA' GROUP BY metodo_pago";
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) totales.put(rs.getString("metodo_pago"), rs.getDouble("suma"));
        } catch (SQLException e) { System.err.println("Error totales caja: " + e.getMessage()); }
        return totales;
    }

    public static void registrarCierre(String cajero, double fondoInicial, double efectivo, double pagosFiado, double digital, double ventasFiadas, double totalFacturado) {
        crearTablaCierres();
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO cierres_caja(cajero,fondo_inicial,ventas_efectivo,pagos_fiado,ventas_digitales,ventas_fiadas,total_facturado) VALUES(?,?,?,?,?,?,?)")) {
            ps.setString(1, cajero); ps.setDouble(2, fondoInicial);
            ps.setDouble(3, efectivo); ps.setDouble(4, pagosFiado);
            ps.setDouble(5, digital);  ps.setDouble(6, ventasFiadas);
            ps.setDouble(7, totalFacturado);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error registrarCierre: " + e.getMessage()); }
    }

    public static void cerrarCaja() {
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement ps = conn.prepareStatement(
                "UPDATE ventas SET estado_caja='CERRADA' WHERE estado_caja='ABIERTA'")) {
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("Error cerrarCaja: " + e.getMessage()); }
    }

    public static List<String[]> obtenerHistorialCierres() {
        crearTablaCierres();
        List<String[]> lista = new ArrayList<>();
        String sql = "SELECT id, datetime(fecha,'localtime') as fl, cajero, fondo_inicial, " +
                     "ventas_efectivo, pagos_fiado, ventas_digitales, ventas_fiadas, total_facturado " +
                     "FROM cierres_caja ORDER BY id DESC";
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next())
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")), rs.getString("fl"), rs.getString("cajero"),
                    "$" + String.format("%.2f", rs.getDouble("fondo_inicial")),
                    "$" + String.format("%.2f", rs.getDouble("ventas_efectivo")),
                    "$" + String.format("%.2f", rs.getDouble("pagos_fiado")),
                    "$" + String.format("%.2f", rs.getDouble("ventas_digitales")),
                    "$" + String.format("%.2f", rs.getDouble("ventas_fiadas")),
                    "$" + String.format("%.2f", rs.getDouble("total_facturado"))
                });
        } catch (SQLException e) { System.err.println("Error historialCierres: " + e.getMessage()); }
        return lista;
    }

    /** Para exportación: lista completa con columnas detalladas */
    public static List<String[]> obtenerHistorialParaExportar(String periodo) {
        crearTablaVentas();
        List<String[]> lista = new ArrayList<>();
        String filtro = switch (periodo) {
            case "hoy"    -> "date(fecha,'localtime') = date('now','localtime')";
            case "semana" -> "fecha >= datetime('now','-7 days')";
            case "mes"    -> "fecha >= datetime('now','-30 days')";
            default       -> "1=1";
        };
        String sql = "SELECT id, datetime(fecha,'localtime'), cliente, detalle, total, costo_total, metodo_pago, cajero " +
                     "FROM ventas WHERE " + filtro + " ORDER BY fecha DESC";
        try (Connection conn = ConexionDB.conectar(); Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                double total = rs.getDouble("total");
                double costo = rs.getDouble("costo_total");
                lista.add(new String[]{
                    String.valueOf(rs.getInt("id")),
                    rs.getString(2),
                    rs.getString("cliente") != null ? rs.getString("cliente") : "-",
                    rs.getString("detalle"),
                    String.format("%.2f", total),
                    String.format("%.2f", costo),
                    String.format("%.2f", total - costo),
                    rs.getString("metodo_pago"),
                    rs.getString("cajero") != null ? rs.getString("cajero") : "Admin"
                });
            }
        } catch (SQLException e) { System.err.println("Error exportar: " + e.getMessage()); }
        return lista;
    }
}
