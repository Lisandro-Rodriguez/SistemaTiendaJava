package com.tienda.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

    //para actualizar, borrar datos de la tabla

    // Método para BORRAR un producto por su código
public static void eliminarProducto(String codigo) {
    String sql = "DELETE FROM productos WHERE codigo_barras = ?";

    try (Connection conn = ConexionDB.conectar();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setString(1, codigo);
        pstmt.executeUpdate();
        System.out.println("Producto eliminado de la BD.");

    } catch (SQLException e) {
        System.out.println("Error al eliminar: " + e.getMessage());
    }
}

    // Método para ACTUALIZAR datos (Nombre, Costo, Stock)
public static void actualizarProducto(Producto p) {
    String sql = "UPDATE productos SET nombre = ?, precio_costo = ?, precio_venta = ?, stock = ? WHERE codigo_barras = ?";

    try (Connection conn = ConexionDB.conectar();
         PreparedStatement pstmt = conn.prepareStatement(sql)) {
        
        pstmt.setString(1, p.getNombre());
        pstmt.setDouble(2, p.getPrecioCosto());
        pstmt.setDouble(3, p.getPrecioVenta());
        pstmt.setInt(4, p.getStock());
        pstmt.setString(5, p.getCodigoBarras());

        pstmt.executeUpdate();
        System.out.println("Producto actualizado en la BD.");

    } catch (SQLException e) {
        System.out.println("Error al actualizar: " + e.getMessage());
    }
}

        // Busca un solo producto por código
    public static Producto buscarPorCodigo(String codigo) {
     String sql = "SELECT * FROM productos WHERE codigo_barras = ?";
        try (Connection conn = ConexionDB.conectar();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
             pstmt.setString(1, codigo);
             ResultSet rs = pstmt.executeQuery();
             if (rs.next()) {
                Producto p = new Producto(rs.getString("codigo_barras"), rs.getString("nombre"), 
                rs.getDouble("precio_costo"), rs.getInt("stock"), 0);
                p.setPrecioVenta(rs.getDouble("precio_venta"));
                return p;
            }
         } catch (SQLException e) { System.out.println(e.getMessage()); }
    return null;
}

        // Resta stock tras una venta
    public static void reducirStock(String codigo, int cantidad) {
        String sql = "UPDATE productos SET stock = stock - ? WHERE codigo_barras = ?";
        try (Connection conn = ConexionDB.conectar();
            PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, cantidad);
            pstmt.setString(2, codigo);
            pstmt.executeUpdate();
        } catch (SQLException e) { System.out.println(e.getMessage()); }
    }
}