package com.tienda.vista;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.tienda.db.VentaDAO;

public class PanelHistorial extends JPanel {

    private DefaultTableModel modelo;

    public PanelHistorial() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        construir();
    }

    private void construir() {
        JPanel panelArriba = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton btnActualizar = new JButton("↻ Actualizar Historial");
        btnActualizar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnActualizar.addActionListener(e -> refrescar());
        panelArriba.add(btnActualizar);
        add(panelArriba, BorderLayout.NORTH);

        String[] cols = {"Nº TICKET", "FECHA", "CLIENTE", "DETALLE DE PRODUCTOS", "TOTAL", "MÉTODO", "CAJERO"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override

            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(30);
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tabla.getColumnModel().getColumn(3).setPreferredWidth(300);

        refrescar();
        add(new JScrollPane(tabla), BorderLayout.CENTER);
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (String[] fila : VentaDAO.obtenerHistorial()) modelo.addRow(fila);
    }
}