package com.tienda.vista;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import com.tienda.db.ProductoDAO;
import com.tienda.Modelo.Producto;
import java.util.List;

public class VentanaInventario extends JFrame {
    private JTable tabla;
    private DefaultTableModel modeloTabla;

    public VentanaInventario() {
        setTitle("Inventario de Productos");
        setSize(700, 450);
        setLocationRelativeTo(null); // Centrar

        // Definimos las columnas de nuestra tabla
        String[] columnas = {"CÓDIGO", "NOMBRE", "COSTO ($)", "VENTA ($)", "STOCK"};
        modeloTabla = new DefaultTableModel(columnas, 0);
        tabla = new JTable(modeloTabla);

        // Cargamos los productos desde la BD
        cargarDatos();

        // Agregamos la tabla con una barra de desplazamiento
        add(new JScrollPane(tabla), BorderLayout.CENTER);

        // Botón para refrescar por si agregas algo nuevo
        JButton btnActualizar = new JButton("Actualizar Tabla");
        btnActualizar.addActionListener(e -> cargarDatos());
        add(btnActualizar, BorderLayout.SOUTH);
    }

    private void cargarDatos() {
        modeloTabla.setRowCount(0); // Limpiar tabla antes de cargar
        List<Producto> productos = ProductoDAO.obtenerTodos();
        
        for (Producto p : productos) {
            Object[] fila = {
                p.getCodigoBarras(),
                p.getNombre(),
                p.getPrecioCosto(),
                // Aquí podrías necesitar un método en Producto para obtener el precio de venta calculado
                p.getPrecioVenta(), 
                p.getStock()
            };
            modeloTabla.addRow(fila);
        }
    }
}