package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.tienda.Modelo.Producto;

public class ProductoDAO {

    /**
     * Guarda un producto en la base de datos.
     */
    public static void registrarProducto(Producto producto) {
        // La instrucción SQL para insertar los datos en las 5 columnas
        String sql = "INSERT INTO productos(codigo_barras, nombre, precio_costo, precio_venta, stock) VALUES(?,?,?,?,?)";

        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Reemplazamos los "?" con los datos reales del producto
            pstmt.setString(1, producto.getCodigoBarras());
            pstmt.setString(2, producto.getNombre());
            pstmt.setDouble(3, producto.getPrecioCosto());
            pstmt.setDouble(4, producto.getPrecioVenta()); // Aquí ya va el precio calculado con tu margen personalizado
            pstmt.setInt(5, producto.getStock());

            // Ejecutamos la orden en la base de datos
            pstmt.executeUpdate();
            System.out.println("¡Éxito! Producto '" + producto.getNombre() + "' guardado en la base de datos.");

        } catch (SQLException e) {
            System.out.println("Error al guardar el producto: " + e.getMessage());
        }
    }

    //Para crear sección de BD

    public static java.util.List<Producto> obtenerTodos() {
    java.util.List<Producto> lista = new java.util.ArrayList<>();
    String sql = "SELECT * FROM productos";

    try (Connection conn = ConexionDB.conectar();
         java.sql.Statement stmt = conn.createStatement();
         java.sql.ResultSet rs = stmt.executeQuery(sql)) {

        while (rs.next()) {
            // Creamos el producto con los datos de la base de datos
            Producto p = new Producto(
                rs.getString("codigo_barras"),
                rs.getString("nombre"),
                rs.getDouble("precio_costo"),
                rs.getInt("stock"),
                0 // El margen no lo necesitamos aquí porque ya tenemos el precio_venta calculado
            );
            
            // IMPORTANTE: Sobrescribimos el precio de venta con el valor real guardado en la BD
            p.setPrecioVenta(rs.getDouble("precio_venta")); 
            lista.add(p);
        }
    } catch (java.sql.SQLException e) {
        System.out.println("Error al obtener productos: " + e.getMessage());
    }
    return lista;
    }
}