package com.tienda.vista;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

import com.tienda.db.VentaDAO;

public class PanelCierresCaja extends JPanel {

    private DefaultTableModel modelo;

    public PanelCierresCaja() {
        setLayout(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(12, 12, 12, 12));
        construir();
    }

    private void construir() {
        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        topBar.setBorder(new EmptyBorder(0, 0, 6, 0));
        JButton btnActualizar = new JButton("↻ Actualizar Lista");
        btnActualizar.setFocusPainted(false);
        btnActualizar.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        topBar.add(btnActualizar);
        add(topBar, BorderLayout.NORTH);

        String[] cols = {"Nº", "FECHA Y HORA", "CAJERO", "FONDO INI.",
                "VENTAS EFECT.", "COBRO FIADOS", "DIGITAL", "VENTAS FIADAS", "TOTAL FACTURADO"};
        modelo = new DefaultTableModel(cols, 0) {
            @Override

            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable tabla = new JTable(modelo);
        tabla.setRowHeight(32);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.getTableHeader().setFont(new Font("SansSerif", Font.BOLD, 13));
        tabla.getTableHeader().setBackground(new Color(45, 55, 72));
        tabla.getTableHeader().setForeground(Color.WHITE);

        // Resaltar columna total
        tabla.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override

            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean sel, boolean focus, int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, sel, focus, row, col);
                if (!sel) {
                    if (col == 8) {
                        c.setFont(c.getFont().deriveFont(Font.BOLD));
                        c.setForeground(new Color(30, 90, 160));
                        c.setBackground(new Color(230, 242, 255));
                    } else {
                        c.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 250, 253));
                        c.setForeground(t.getForeground());
                    }
                }
                return c;
            }
        });

        refrescar();
        btnActualizar.addActionListener(e -> refrescar());

        JScrollPane scroll = new JScrollPane(tabla);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(200, 210, 225)));
        add(scroll, BorderLayout.CENTER);
    }

    public void refrescar() {
        modelo.setRowCount(0);
        for (String[] fila : VentaDAO.obtenerHistorialCierres()) modelo.addRow(fila);
    }
}